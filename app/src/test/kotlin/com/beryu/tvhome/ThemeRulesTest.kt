package com.beryu.tvhome

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeRulesTest {
    @Test
    fun `theme boundaries follow local time through midnight and year changes`() = withRestoredTimeZone {
        for (zone in listOf("UTC", "Asia/Tokyo", "America/Los_Angeles", "Asia/Kathmandu")) {
            TimeZone.setDefault(TimeZone.getTimeZone(zone))
            checkLocal("2026-10-24T17:59:59.999999999", false, "2026-10-24T18:00:00")
            checkLocal("2026-10-24T18:00:00", true, "2026-10-25T06:00:00")
            checkLocal("2026-10-24T23:59:59", true, "2026-10-25T06:00:00")
            checkLocal("2026-10-25T00:00:00", true, "2026-10-25T06:00:00")
            checkLocal("2026-10-25T05:59:59.999999999", true, "2026-10-25T06:00:00")
            checkLocal("2026-10-25T06:00:00", false, "2026-10-25T18:00:00")
            checkLocal("2026-12-31T18:00:00", true, "2027-01-01T06:00:00")
            checkLocal("2027-01-01T06:00:00", false, "2027-01-01T18:00:00")
        }
    }

    @Test
    fun `the same instant uses the updated device time zone`() = withRestoredTimeZone {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"))
        checkInstant("2026-10-24T09:00:00Z", true, "2026-10-24T21:00:00Z")
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        checkInstant("2026-10-24T09:00:00Z", false, "2026-10-24T18:00:00Z")
        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
        checkInstant("2026-10-24T09:00:00Z", true, "2026-10-24T13:00:00Z")
    }

    @Test
    fun `daylight saving changes the duration until the next boundary`() = withRestoredTimeZone {
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
        checkInstant("2026-03-07T23:00:00Z", true, "2026-03-08T10:00:00Z")
        checkInstant("2026-10-31T22:00:00Z", true, "2026-11-01T11:00:00Z")
    }

    private fun withRestoredTimeZone(block: () -> Unit) {
        val original = TimeZone.getDefault()
        try {
            block()
        } finally {
            TimeZone.setDefault(original)
        }
    }

    private fun checkLocal(value: String, dark: Boolean, next: String) {
        val zone = ZoneId.systemDefault()
        checkInstant(
            LocalDateTime.parse(value).atZone(zone).toInstant().toString(), dark,
            LocalDateTime.parse(next).atZone(zone).toInstant().toString(),
        )
    }

    private fun checkInstant(value: String, dark: Boolean, next: String) {
        val now = Instant.parse(value)
        val message = "Theme boundary at $value in ${TimeZone.getDefault().id}"
        assertEquals(message, dark, ThemeRules.isDark(now))
        assertEquals(message, Instant.parse(next), ThemeRules.nextChange(now))
    }
}
