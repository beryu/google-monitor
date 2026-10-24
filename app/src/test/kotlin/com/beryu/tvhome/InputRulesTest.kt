package com.beryu.tvhome

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class InputRulesTest {
    @Test
    fun `brand matching preserves priority and accepts missing labels`() {
        val cases = listOf(
            "Nintendo Switch 2" to InputRules.Brand.NINTENDO,
            "HDMI 2 · PlayStation 5" to InputRules.Brand.PLAYSTATION,
            "switch" to InputRules.Brand.NINTENDO,
            "PLAYSTATION" to InputRules.Brand.PLAYSTATION,
            "PS5" to InputRules.Brand.NONE,
            null to InputRules.Brand.NONE,
            "PC" to InputRules.Brand.NONE,
            "Switch PlayStation" to InputRules.Brand.NINTENDO,
        )
        for ((label, expected) in cases) assertEquals(label, expected, InputRules.brand(label))
    }

    @Test
    fun `custom names retain whitespace and blank names use fallback`() {
        assertEquals("  Switch 2  ", InputRules.label("  Switch 2  ", "HDMI 1"))
        assertEquals("HDMI 1", InputRules.label("  ", "HDMI 1"))
        assertEquals("PlayStation", InputRules.label(null, "PlayStation"))
        assertEquals("HDMI", InputRules.label(null, null))
        // Match Java String.trim behavior, including its ASCII-only whitespace boundary.
        assertEquals("HDMI 1", InputRules.label("\u0000\t ", "HDMI 1"))
        assertEquals("\u2003", InputRules.label("\u2003", "HDMI 1"))
    }

    @Test
    fun `brand matching is independent of the device locale`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale("tr", "TR"))
            assertEquals(InputRules.Brand.NINTENDO, InputRules.brand("SWITCH"))
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun `input equality retains home cards when id and label are unchanged`() {
        assertEquals(InputEntry("hdmi-1", "Switch"), InputEntry("hdmi-1", "Switch"))
        org.junit.Assert.assertNotEquals(InputEntry("hdmi-1", "Switch"), InputEntry("hdmi-1", "PC"))
        org.junit.Assert.assertNotEquals(InputEntry("hdmi-1", "Switch"), InputEntry("hdmi-2", "Switch"))
    }
}
