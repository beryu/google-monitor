package com.beryu.tvhome;

public final class HomeRedirectRulesTest {
    public static void main(String[] args) {
        String launcher = "com.google.android.apps.tv.launcherx";
        check(HomeRedirectRules.shouldRedirect(launcher, launcher + ".home.HomeActivity"));
        check(HomeRedirectRules.shouldRedirect(launcher, launcher + ".home.VanillaModeHomeActivity"));
        check(!HomeRedirectRules.shouldRedirect(launcher, launcher + ".dashboard.DashboardHandler"));
        check(!HomeRedirectRules.shouldRedirect(launcher, launcher + ".settings.SettingsActivity"));
        check(!HomeRedirectRules.shouldRedirect("com.android.tv.settings", "com.android.tv.settings.MainSettings"));
        check(!HomeRedirectRules.shouldRedirect("com.beryu.tvhome", "com.beryu.tvhome.MainActivity"));
        check(!HomeRedirectRules.shouldRedirect("com.mitv.livetv", "com.mitv.livetv.tv.NoTunerMainActivity"));
        check(!HomeRedirectRules.shouldRedirect("other.package", launcher + ".home.HomeActivity"));
        check(!HomeRedirectRules.shouldRedirect(launcher, null));
        check(!HomeRedirectRules.shouldRedirect(null, launcher + ".home.HomeActivity"));
        System.out.println("10 home redirect isolation checks passed.");
    }

    private static void check(boolean value) {
        if (!value) throw new AssertionError("Home redirect rule failed");
    }
}
