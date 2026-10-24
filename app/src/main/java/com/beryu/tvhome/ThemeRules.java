package com.beryu.tvhome;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/** Read the device's current time zone on each check, including after a zone change. */
final class ThemeRules {
    static boolean isDark(Instant now) {
        int hour = now.atZone(ZoneId.systemDefault()).getHour();
        return hour >= 18 || hour < 6;
    }

    static Instant nextChange(Instant now) {
        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime local = now.atZone(zone);
        int hour = local.getHour();
        ZonedDateTime next = hour >= 18
                ? local.toLocalDate().plusDays(1).atTime(6, 0).atZone(zone)
                : local.toLocalDate().atTime(hour < 6 ? 6 : 18, 0).atZone(zone);
        return next.toInstant();
    }

    private ThemeRules() {}
}
