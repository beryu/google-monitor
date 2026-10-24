package com.beryu.tvhome

/** Only the stock home surface is redirected; its dashboard and settings must stay usable. */
internal object HomeRedirectRules {
    private const val LAUNCHER = "com.google.android.apps.tv.launcherx"

    fun shouldRedirect(packageName: CharSequence?, className: CharSequence?): Boolean =
        packageName?.toString() == LAUNCHER && when (className?.toString()) {
            "$LAUNCHER.home.HomeActivity", "$LAUNCHER.home.VanillaModeHomeActivity" -> true
            else -> false
        }
}
