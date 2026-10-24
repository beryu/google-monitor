package com.beryu.tvhome;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.TimeZone;

public final class ThemeRulesTest {
    public static void main(String[] args) {
        TimeZone original = TimeZone.getDefault();
        try {
            for (String zone : new String[]{"UTC", "Asia/Tokyo", "America/Los_Angeles", "Asia/Kathmandu"}) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone));
                checkLocal("2026-10-24T17:59:59.999999999", false, "2026-10-24T18:00:00");
                checkLocal("2026-10-24T18:00:00", true, "2026-10-25T06:00:00");
                checkLocal("2026-10-24T23:59:59", true, "2026-10-25T06:00:00");
                checkLocal("2026-10-25T00:00:00", true, "2026-10-25T06:00:00");
                checkLocal("2026-10-25T05:59:59.999999999", true, "2026-10-25T06:00:00");
                checkLocal("2026-10-25T06:00:00", false, "2026-10-25T18:00:00");
                checkLocal("2026-12-31T18:00:00", true, "2027-01-01T06:00:00");
                checkLocal("2027-01-01T06:00:00", false, "2027-01-01T18:00:00");
            }
            // The same instant changes theme and schedule immediately when the default zone changes.
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));
            check("2026-10-24T09:00:00Z", true, "2026-10-24T21:00:00Z");
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            check("2026-10-24T09:00:00Z", false, "2026-10-24T18:00:00Z");
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));
            check("2026-10-24T09:00:00Z", true, "2026-10-24T13:00:00Z");
            // DST makes the overnight interval shorter or longer than twelve hours.
            TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
            check("2026-03-07T23:00:00Z", true, "2026-03-08T10:00:00Z");
            check("2026-10-31T22:00:00Z", true, "2026-11-01T11:00:00Z");
        } finally {
            TimeZone.setDefault(original);
        }
        System.out.println("37 local-time theme boundary, zone change, and DST checks passed.");
    }

    private static void checkLocal(String value, boolean dark, String next) {
        ZoneId zone = ZoneId.systemDefault();
        check(LocalDateTime.parse(value).atZone(zone).toInstant().toString(), dark,
                LocalDateTime.parse(next).atZone(zone).toInstant().toString());
    }

    private static void check(String value, boolean dark, String next) {
        Instant now = Instant.parse(value);
        if (ThemeRules.isDark(now) != dark || !ThemeRules.nextChange(now).equals(Instant.parse(next))) {
            throw new AssertionError("Theme boundary failed at " + value + " in " + TimeZone.getDefault().getID());
        }
    }
}
