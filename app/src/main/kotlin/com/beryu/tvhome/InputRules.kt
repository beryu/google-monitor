package com.beryu.tvhome

import java.util.Locale

/** Pure rules kept independent of Android so ambiguous names are testable. */
internal object InputRules {
    enum class Brand { NINTENDO, PLAYSTATION, NONE }

    fun brand(label: String?): Brand {
        val normalized = label.orEmpty().lowercase(Locale.ROOT)
        return when {
            "switch" in normalized -> Brand.NINTENDO
            "playstation" in normalized -> Brand.PLAYSTATION
            else -> Brand.NONE
        }
    }

    fun label(custom: CharSequence?, fallback: CharSequence?): String =
        custom?.toString()?.takeIf { it.trim { char -> char <= ' ' }.isNotEmpty() }
            ?: fallback?.toString()
            ?: "HDMI"
}
