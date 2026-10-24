package com.beryu.tvhome

import java.time.Instant
import java.time.ZoneId

/** Read the device's current time zone on each check, including after a zone change. */
internal object ThemeRules {
    fun isDark(now: Instant): Boolean {
        val hour = now.atZone(ZoneId.systemDefault()).hour
        return hour >= 18 || hour < 6
    }

    fun nextChange(now: Instant): Instant {
        val zone = ZoneId.systemDefault()
        val local = now.atZone(zone)
        val date = local.toLocalDate()
        val next = when {
            local.hour >= 18 -> date.plusDays(1).atTime(6, 0)
            local.hour < 6 -> date.atTime(6, 0)
            else -> date.atTime(18, 0)
        }
        return next.atZone(zone).toInstant()
    }
}
