package com.beryu.tvhome;

/** Only the stock home surface is redirected; its dashboard and settings must stay usable. */
public final class HomeRedirectRules {
    private HomeRedirectRules() {}

    public static boolean shouldRedirect(CharSequence packageName, CharSequence className) {
        if (packageName == null || className == null) return false;
        if (!"com.google.android.apps.tv.launcherx".contentEquals(packageName)) return false;
        return "com.google.android.apps.tv.launcherx.home.HomeActivity".contentEquals(className)
                || "com.google.android.apps.tv.launcherx.home.VanillaModeHomeActivity".contentEquals(className);
    }
}
