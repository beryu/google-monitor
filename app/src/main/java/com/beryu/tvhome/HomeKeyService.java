package com.beryu.tvhome;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

/** Optional, separately enabled key filter; window contents and key values are never recorded. */
public final class HomeKeyService extends AccessibilityService {
    private final HomeKeyRules keys = new HomeKeyRules();

    @Override protected boolean onKeyEvent(KeyEvent event) {
        HomeKeyRules.Decision decision = keys.onKey(event.getKeyCode(), event.getAction(), event.getRepeatCount());
        if (decision == HomeKeyRules.Decision.PASS) return false;
        if (decision == HomeKeyRules.Decision.OPEN_HOME) {
            try {
                startActivity(new Intent(this, MainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                                | Intent.FLAG_ACTIVITY_SINGLE_TOP));
            } catch (RuntimeException e) {
                keys.reset();
                Log.w("QuietTV", "Direct Home unavailable; letting the system handle Home", e);
                return false;
            }
        }
        return true;
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) { }
    @Override public void onInterrupt() { keys.reset(); }
}
