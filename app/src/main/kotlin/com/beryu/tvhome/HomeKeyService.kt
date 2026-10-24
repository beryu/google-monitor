package com.beryu.tvhome

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/** Optional, separately enabled key filter; window contents and key values are never recorded. */
class HomeKeyService : AccessibilityService() {
    private val keys = HomeKeyRules()

    override fun onKeyEvent(event: KeyEvent): Boolean {
        when (keys.onKey(event.keyCode, event.action, event.repeatCount)) {
            HomeKeyRules.Decision.PASS -> return false
            HomeKeyRules.Decision.CONSUME -> return true
            HomeKeyRules.Decision.OPEN_HOME -> {
                try {
                    startActivity(Intent(this, MainActivity::class.java).addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                    ))
                } catch (error: RuntimeException) {
                    keys.reset()
                    Log.w("QuietTV", "Direct Home unavailable; letting the system handle Home", error)
                    return false
                }
            }
        }
        return true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() {
        keys.reset()
    }
}
