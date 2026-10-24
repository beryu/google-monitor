package com.beryu.tvhome

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeKeyRulesTest {
    @Test
    fun `only paired Home events are consumed and failed launches reset the pair`() {
        val keys = HomeKeyRules()
        val events = listOf(
            Triple(24, 0, 0) to HomeKeyRules.Decision.PASS, // Volume up
            Triple(83, 0, 0) to HomeKeyRules.Decision.PASS, // Dashboard
            Triple(176, 1, 0) to HomeKeyRules.Decision.PASS, // Settings
            Triple(3, 1, 0) to HomeKeyRules.Decision.PASS, // No captured down
            Triple(3, 0, 2) to HomeKeyRules.Decision.PASS, // Orphan repeat
            Triple(3, 0, 0) to HomeKeyRules.Decision.OPEN_HOME,
            Triple(3, 0, 1) to HomeKeyRules.Decision.CONSUME,
            Triple(3, 0, 10) to HomeKeyRules.Decision.CONSUME,
            Triple(25, 0, 0) to HomeKeyRules.Decision.PASS, // Other keys while Home held
            Triple(3, 1, 0) to HomeKeyRules.Decision.CONSUME,
            Triple(3, 1, 0) to HomeKeyRules.Decision.PASS,
            Triple(3, 0, 0) to HomeKeyRules.Decision.OPEN_HOME,
        )
        for ((event, expected) in events) {
            assertEquals(event.toString(), expected, keys.onKey(event.first, event.second, event.third))
        }
        keys.reset() // Launch failed or service interrupted
        assertEquals(HomeKeyRules.Decision.PASS, keys.onKey(3, 1, 0))
        assertEquals(HomeKeyRules.Decision.PASS, keys.onKey(3, 2, 0))
    }
}
