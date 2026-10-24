package com.beryu.tvhome

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeRedirectRulesTest {
    @Test
    fun `only stock home surfaces are redirected`() {
        val launcher = "com.google.android.apps.tv.launcherx"
        assertTrue(HomeRedirectRules.shouldRedirect(launcher, "$launcher.home.HomeActivity"))
        assertTrue(HomeRedirectRules.shouldRedirect(launcher, "$launcher.home.VanillaModeHomeActivity"))
        assertFalse(HomeRedirectRules.shouldRedirect(launcher, "$launcher.dashboard.DashboardHandler"))
        assertFalse(HomeRedirectRules.shouldRedirect(launcher, "$launcher.settings.SettingsActivity"))
        assertFalse(HomeRedirectRules.shouldRedirect("com.android.tv.settings", "com.android.tv.settings.MainSettings"))
        assertFalse(HomeRedirectRules.shouldRedirect("com.beryu.tvhome", "com.beryu.tvhome.MainActivity"))
        assertFalse(HomeRedirectRules.shouldRedirect("com.mitv.livetv", "com.mitv.livetv.tv.NoTunerMainActivity"))
        assertFalse(HomeRedirectRules.shouldRedirect("other.package", "$launcher.home.HomeActivity"))
        assertFalse(HomeRedirectRules.shouldRedirect(launcher, null))
        assertFalse(HomeRedirectRules.shouldRedirect(null, "$launcher.home.HomeActivity"))
    }
}
