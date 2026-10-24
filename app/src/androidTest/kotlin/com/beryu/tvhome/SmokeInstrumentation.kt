package com.beryu.tvhome

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.GridView
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

/** Explicitly invoked smoke checks. Fixtures never appear during normal launches. */
class SmokeInstrumentation : Instrumentation() {
    private var benchmark = false

    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        benchmark = arguments?.getString("benchmark") == "true"
        start()
    }

    override fun onStart() {
        val result = Bundle()
        try {
            val activity = startActivitySync(Intent(targetContext, MainActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            )) as MainActivity
            waitForIdleSync()
            if (benchmark) {
                benchmarkHomeReturn(activity, result)
                finish(Activity.RESULT_OK, result)
                return
            }
            checkNavigation(activity)
            checkHomeRows(activity)
            checkHomeReuse(activity)
            checkThemeChanges(activity)
            // Return to the real input list after the fixtures; leave the real home visible.
            runOnMainSync { activity.showApps() }
            pressKey(KeyEvent.KEYCODE_BACK)
            result.putString("stream", "PASS: theme changes, home reuse, input changes, Home lifecycle, focus, D-pad movement, app drawer, settings, generic HDMI, four-card row, wrapping, Back\n")
            finish(Activity.RESULT_OK, result)
        } catch (error: Throwable) {
            result.putString("stream", "FAIL: $error\n")
            finish(Activity.RESULT_CANCELED, result)
        }
    }

    private fun checkNavigation(activity: MainActivity) {
        runOnMainSync {
            activity.renderHome(listOf(
                InputEntry("fixture-hdmi-1", "Nintendo Switch 2"),
                InputEntry("fixture-hdmi-2", "PlayStation 5"),
            ), null)
        }
        waitForIdleSync()
        runOnMainSync { verify(activity.currentFocus?.tag == "fixture-hdmi-1", "Initial focus") }
        screenshot("home-preview.png")
        pressKey(KeyEvent.KEYCODE_DPAD_RIGHT)
        runOnMainSync { verify(activity.currentFocus?.tag == "fixture-hdmi-2", "Right to PlayStation") }
        pressKey(KeyEvent.KEYCODE_DPAD_RIGHT)
        runOnMainSync { verify(activity.currentFocus?.tag == "apps", "Right to apps") }
        pressKey(KeyEvent.KEYCODE_DPAD_CENTER)
        runOnMainSync {
            val grid = requireGrid(activity)
            verify(grid.count > 0, "App drawer contains launchable apps")
            verify(grid.hasFocus(), "Grid has D-pad focus")
            verify(grid.selectedItemPosition == 0, "First app selected")
        }
        screenshot("apps-preview.png")
        pressKey(KeyEvent.KEYCODE_DPAD_RIGHT)
        runOnMainSync {
            val grid = requireGrid(activity)
            if (grid.count > 1) verify(grid.selectedItemPosition == 1, "Grid D-pad movement")
        }
        pressKey(KeyEvent.KEYCODE_DPAD_UP)
        runOnMainSync { verify(activity.currentFocus?.tag == "tv-settings", "Settings reachable with D-pad") }
        val settings = addMonitor(IntentFilter(Settings.ACTION_SETTINGS), ActivityResult(0, null), true)
        try {
            pressKey(KeyEvent.KEYCODE_DPAD_CENTER)
            verify(settings.hits == 1, "Settings action launched")
        } finally {
            removeMonitor(settings)
        }
        pressKey(KeyEvent.KEYCODE_BACK)
        runOnMainSync { verify(activity.currentFocus?.tag == "apps", "Back restores apps focus") }
    }

    private fun checkHomeRows(activity: MainActivity) {
        runOnMainSync {
            activity.renderHome(listOf(
                InputEntry("fixture-hdmi-1", "Nintendo Switch 2"),
                InputEntry("fixture-hdmi-2", "PlayStation 5"),
                InputEntry("fixture-hdmi-3", "Aux"),
            ), null)
        }
        waitForIdleSync()
        runOnMainSync {
            val first = activity.tile("fixture-hdmi-1")
            val generic = activity.tile("fixture-hdmi-3")
            val apps = activity.tile("apps")
            verify(generic.isFocusable, "Generic HDMI input is accessible")
            verify(first.parent === generic.parent && first.parent === apps.parent,
                "Three inputs and All Apps fit on one row")
            verify(abs(first.width - apps.width) <= 1, "Cards have equal width")
            generic.requestFocus()
        }
        pressKey(KeyEvent.KEYCODE_DPAD_RIGHT)
        runOnMainSync { verify(activity.currentFocus?.tag == "apps", "Generic HDMI navigates to All Apps") }
        screenshot("four-card-preview.png")
        runOnMainSync {
            activity.renderHome((1..4).map { InputEntry("fixture-hdmi-$it", "HDMI $it") }, null)
        }
        waitForIdleSync()
        runOnMainSync {
            val first = activity.tile("fixture-hdmi-1")
            val apps = activity.tile("apps")
            verify(first.parent !== apps.parent, "Additional inputs wrap to another row")
            verify(abs(first.width - apps.width) <= 1, "Final row keeps card width")
            activity.tile("fixture-hdmi-4").requestFocus()
        }
        pressKey(KeyEvent.KEYCODE_DPAD_DOWN)
        runOnMainSync { verify(activity.currentFocus?.tag == "apps", "Wrapped row remains reachable with D-pad") }
    }

    private fun checkThemeChanges(activity: MainActivity) {
        val zone = ZoneId.systemDefault()
        val day = LocalDate.of(2026, 10, 24).atTime(17, 59, 59).atZone(zone).toInstant()
        val night = LocalDate.of(2026, 10, 24).atTime(18, 0).atZone(zone).toInstant()
        val inputs = listOf(
            InputEntry("theme-1", "Nintendo Switch 2"),
            InputEntry("theme-2", "PlayStation 5"),
            InputEntry("theme-3", "HDMI 3"),
        )
        lateinit var homeTile: View
        lateinit var drawer: GridView
        runOnMainSync {
            activity.renderHome(inputs, null)
            homeTile = activity.tile("theme-2")
            homeTile.requestFocus()
            activity.updateTheme(day)
        }
        waitForIdleSync()
        screenshot("home-light-preview.png")
        runOnMainSync {
            verifyTileColor(homeTile, ThemePalette.LIGHT.focusedCard)
            activity.updateTheme(night)
            verifyTileColor(homeTile, ThemePalette.DARK.focusedCard)
            verify(activity.currentFocus === homeTile, "Theme preserves home focus")
            verify(activity.tile("theme-2") === homeTile, "Theme reuses home cards")
        }
        waitForIdleSync()
        screenshot("home-dark-preview.png")
        runOnMainSync {
            activity.showApps()
            drawer = requireGrid(activity)
            // Earlier settings checks intentionally retain focus on the settings button.
            drawer.requestFocus()
            drawer.setSelection(minOf(1, drawer.count - 1))
        }
        waitForIdleSync()
        runOnMainSync {
            val position = drawer.selectedItemPosition
            activity.updateTheme(day)
            verify(requireGrid(activity) === drawer, "Theme retains drawer")
            verify(drawer.selectedItemPosition == position, "Theme preserves app selection")
            verify(drawer.hasFocus(), "Theme preserves drawer focus")
            activity.updateTheme(night)
            val settings = activity.tile("tv-settings")
            settings.requestFocus()
            activity.updateTheme(day)
            verify(activity.currentFocus === settings, "Theme preserves settings focus")
            activity.updateTheme(night)
        }
        waitForIdleSync()
        screenshot("apps-dark-preview.png")
        runOnMainSync {
            activity.renderHome(inputs, null)
            verify(activity.tile("theme-2") === homeTile, "Theme updates retained home while drawer is visible")
            // Change only this process's default zone; leave the TV's system settings intact.
            val original = TimeZone.getDefault()
            try {
                val sameTime = Instant.parse("2026-10-24T09:00:00Z")
                TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
                activity.updateTheme(sameTime)
                verifyTileColor(homeTile, ThemePalette.LIGHT.focusedCard)
                TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"))
                activity.updateTheme(sameTime)
                verifyTileColor(homeTile, ThemePalette.DARK.focusedCard)
                verify(activity.currentFocus === homeTile, "Zone change preserves home focus")
            } finally {
                TimeZone.setDefault(original)
                activity.updateTheme(Instant.now())
            }
        }
    }

    private fun checkHomeReuse(activity: MainActivity) {
        runOnMainSync {
            val inputs = listOf(InputEntry("reuse-1", "Switch"), InputEntry("reuse-2", "PlayStation"))
            activity.renderHome(inputs, null)
            val tile = activity.tile("reuse-2")
            tile.requestFocus()
            activity.renderHome(listOf(InputEntry("reuse-1", "Switch"), InputEntry("reuse-2", "PlayStation")), null)
            verify(activity.tile("reuse-2") === tile, "Unchanged inputs reuse cards")
            verify(activity.currentFocus === tile, "Reuse preserves focused input")
            val apps = activity.tile("apps")
            apps.performClick()
            activity.renderHome(inputs, null)
            verify(activity.tile("apps") === apps, "Drawer return reuses home cards")
            verify(activity.currentFocus === apps, "Drawer return focuses All Apps")
            activity.renderHome(listOf(InputEntry("reuse-1", "PC"), InputEntry("reuse-2", "PlayStation")), null)
            verify(activity.tile("reuse-1").contentDescription.toString() == "PC", "Renamed input updates")
            activity.renderHome(listOf(InputEntry("reuse-1", "PC")), null)
            verify(activity.window.decorView.findViewWithTag<View>("reuse-2") == null,
                "Hidden or removed input disappears")
            activity.renderHome(emptyList(), "Input error")
            val errorRoot = activity.tile("apps")
            activity.renderHome(emptyList(), null)
            verify(activity.tile("apps") !== errorRoot, "Error recovery refreshes home")
        }
        // Exercise Android's real onNewIntent -> onResume delivery as well.
        relaunchHome(activity)
        lateinit var apps: View
        runOnMainSync {
            apps = activity.tile("apps")
            apps.performClick()
        }
        relaunchHome(activity)
        runOnMainSync {
            verify(findGrid(activity.window.decorView) == null, "Home closes drawer")
            verify(activity.tile("apps") === apps, "Home intent restores retained home")
        }
    }

    private fun relaunchHome(activity: MainActivity) {
        // startActivitySync waits for onCreate and cannot wait for an existing singleTask.
        val monitor = addMonitor(MainActivity::class.java.name, null, false)
        try {
            runOnMainSync {
                activity.startActivity(Intent(activity, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            verify(waitForMonitorWithTimeout(monitor, 5000) === activity, "Home intent resumes same activity")
            waitForIdleSync()
        } finally {
            removeMonitor(monitor)
        }
    }

    private fun benchmarkHomeReturn(activity: MainActivity, result: Bundle) {
        // Both APK versions run exactly this sequence. Measures synchronous app work,
        // not physical remote latency, window animation, TV wake or cold startup.
        val samples = LongArray(31)
        var replaced = 0
        for (sample in -5 until samples.size) {
            runOnMainSync {
                val apps = activity.tile("apps")
                val start = SystemClock.elapsedRealtimeNanos()
                callActivityOnNewIntent(activity, Intent(targetContext, MainActivity::class.java))
                callActivityOnResume(activity)
                val elapsed = SystemClock.elapsedRealtimeNanos() - start
                if (sample >= 0) {
                    samples[sample] = elapsed
                    if (apps !== activity.tile("apps")) replaced++
                }
            }
            waitForIdleSync()
        }
        samples.sort()
        result.putString("stream", String.format(Locale.ROOT,
            "Home return app work: median=%.3f ms, p90=%.3f ms, card replacements=%d/31 (5 warmups)\n",
            samples[15] / 1_000_000.0, samples[27] / 1_000_000.0, replaced,
        ))
    }

    private fun pressKey(keyCode: Int) {
        sendKeyDownUpSync(keyCode)
        waitForIdleSync()
    }

    private fun screenshot(name: String) {
        // UI idleness can precede the next display frame on this TV.
        waitForIdleSync()
        SystemClock.sleep(250)
        val bitmap = uiAutomation.takeScreenshot() ?: throw AssertionError("Screenshot available")
        try {
            File(targetContext.filesDir, name).outputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
        } finally {
            bitmap.recycle()
        }
    }

    private fun verifyTileColor(view: View, expected: Int) {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        try {
            view.draw(Canvas(bitmap))
            verify(bitmap.getPixel(view.width / 2, view.height / 5) == expected,
                "Theme updates rendered card color")
        } finally {
            bitmap.recycle()
        }
    }

    private fun MainActivity.tile(tag: String): View =
        window.decorView.findViewWithTag<View>(tag) ?: throw AssertionError("Missing view: $tag")

    private fun requireGrid(activity: MainActivity): GridView =
        findGrid(activity.window.decorView) ?: throw AssertionError("App drawer grid is present")

    private fun findGrid(view: View): GridView? {
        if (view is GridView) return view
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                findGrid(view.getChildAt(index))?.let { return it }
            }
        }
        return null
    }

    private fun verify(condition: Boolean, message: String) {
        if (!condition) throw AssertionError(message)
    }
}
