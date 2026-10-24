package com.beryu.tvhome

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent

/** Keeps the vendor home package enabled so global keys and system providers still work. */
class HomeRedirectService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            !HomeRedirectRules.shouldRedirect(event.packageName, event.className)
        ) return

        val home = Intent(this, MainActivity::class.java).addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        )
        try {
            startActivity(home)
        } catch (error: RuntimeException) {
            Log.w("QuietTV", "Home redirect unavailable; stock home remains usable", error)
        }
    }

    override fun onInterrupt() = Unit
}
