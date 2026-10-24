package com.beryu.tvhome

/** Keeps Home down/up paired; every other key passes through unchanged. */
internal class HomeKeyRules {
    enum class Decision { PASS, OPEN_HOME, CONSUME }

    private var handlingHome = false

    fun onKey(keyCode: Int, action: Int, repeatCount: Int): Decision {
        if (keyCode != KEYCODE_HOME) return Decision.PASS
        return when (action) {
            ACTION_DOWN -> when {
                repeatCount == 0 -> {
                    handlingHome = true
                    Decision.OPEN_HOME
                }
                handlingHome -> Decision.CONSUME
                else -> Decision.PASS
            }
            ACTION_UP -> {
                val handled = handlingHome
                handlingHome = false
                if (handled) Decision.CONSUME else Decision.PASS
            }
            else -> Decision.PASS
        }
    }

    fun reset() {
        handlingHome = false
    }

    private companion object {
        // Android's stable constants; keep these rules independent of Android.
        const val KEYCODE_HOME = 3
        const val ACTION_DOWN = 0
        const val ACTION_UP = 1
    }
}
