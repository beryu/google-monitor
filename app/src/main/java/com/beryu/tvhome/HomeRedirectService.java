package com.beryu.tvhome;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

/** Keeps the vendor home package enabled so global keys and system providers still work. */
public final class HomeRedirectService extends AccessibilityService {
    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                || !HomeRedirectRules.shouldRedirect(event.getPackageName(), event.getClassName())) return;
        Intent home = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        try { startActivity(home); }
        catch (RuntimeException e) { Log.w("QuietTV", "Home redirect unavailable; stock home remains usable", e); }
    }

    @Override public void onInterrupt() { }
}
