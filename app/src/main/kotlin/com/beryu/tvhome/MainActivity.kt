package com.beryu.tvhome

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ResolveInfo
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.media.tv.TvContract
import android.media.tv.TvInputManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.FrameLayout
import android.widget.GridView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import java.text.Collator
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt

class MainActivity : Activity() {
    private var inputManager: TvInputManager? = null
    private val handler = Handler(Looper.getMainLooper())
    private var drawerOpen = false
    private var selectedInput = ""
    private var appSelection = 0
    private var grid: GridView? = null
    // Keep API 33 types out of fields initialized on older devices.
    private var backCallback: Any? = null
    private var settingsFocused = false
    private var currentRoot: LinearLayout? = null
    private var homeRoot: LinearLayout? = null
    private var homeEntries: List<InputEntry>? = null
    private var homeError: String? = null
    private var homeMessage: TextView? = null
    private var theme = if (ThemeRules.isDark(Instant.now())) ThemePalette.DARK else ThemePalette.LIGHT
    private var clockRegistered = false

    private val themeRefresh = Runnable { refreshTheme() }
    private val clockChanges = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = refreshTheme()
    }
    private val refresh = Runnable { if (!drawerOpen) loadHome() }
    private val callback = object : TvInputManager.TvInputCallback() {
        override fun onInputAdded(inputId: String) = scheduleRefresh()
        override fun onInputRemoved(inputId: String) = scheduleRefresh()
        override fun onInputUpdated(inputId: String) = scheduleRefresh()
        override fun onInputStateChanged(inputId: String, state: Int) = scheduleRefresh()
    }
    private val packages = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (drawerOpen) showApps()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawable(ColorDrawable(theme.backgroundStart))
        savedInstanceState?.let { state ->
            drawerOpen = state.getBoolean("drawer")
            selectedInput = state.getString("input", "").orEmpty()
            appSelection = state.getInt("app")
        }
        inputManager = getSystemService(TV_INPUT_SERVICE) as TvInputManager?
        inputManager?.registerCallback(callback, handler)
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(packages, filter, Context.RECEIVER_NOT_EXPORTED)
            val callback = OnBackInvokedCallback { returnHome() }
            backCallback = callback
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT, callback,
            )
        } else {
            registerReceiver(packages, filter)
        }
    }

    override fun onResume() {
        super.onResume()
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        if (!clockRegistered) {
            val filter = IntentFilter(Intent.ACTION_TIME_CHANGED).apply {
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
            }
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(clockChanges, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(clockChanges, filter)
            }
            clockRegistered = true
        }
        refreshTheme()
        if (drawerOpen) showApps() else loadHome()
    }

    override fun onPause() {
        handler.removeCallbacks(themeRefresh)
        if (clockRegistered) {
            unregisterReceiver(clockChanges)
            clockRegistered = false
        }
        super.onPause()
    }

    private fun refreshTheme() {
        val now = Instant.now()
        updateTheme(now)
        // One callback at the next boundary; no polling while visible or in the background.
        handler.removeCallbacks(themeRefresh)
        val delay = Duration.between(now, ThemeRules.nextChange(now)).toMillis().coerceAtLeast(1)
        handler.postDelayed(themeRefresh, delay)
    }

    // Internal for deterministic boundary and focus checks on a device.
    internal fun updateTheme(now: Instant) {
        val next = if (ThemeRules.isDark(now)) ThemePalette.DARK else ThemePalette.LIGHT
        if (theme === next) return
        theme = next
        window.setBackgroundDrawable(ColorDrawable(theme.backgroundStart))
        homeRoot?.let { applyThemeToRoot(it) }
        currentRoot?.takeIf { it !== homeRoot }?.let { applyThemeToRoot(it) }
    }

    private fun applyThemeToRoot(root: LinearLayout) {
        root.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR, intArrayOf(theme.backgroundStart, theme.backgroundEnd),
        )
        applyThemeToChildren(root)
    }

    private fun applyThemeToChildren(view: View) {
        when (view) {
            is TileView -> view.applyTheme(theme)
            is TextView -> {
                view.setTextColor(if (view === homeMessage) theme.muted else theme.foreground)
                if (view is Button) {
                    view.backgroundTintList = ColorStateList(
                        arrayOf(
                            intArrayOf(android.R.attr.state_pressed),
                            intArrayOf(android.R.attr.state_focused),
                            intArrayOf(),
                        ),
                        intArrayOf(theme.focusedButton, theme.focusedButton, theme.button),
                    )
                }
            }
        }
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) applyThemeToChildren(view.getChildAt(index))
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Home always closes the drawer. Android calls onResume next, where inputs
        // are refreshed once instead of reading inputs and building the screen twice.
        drawerOpen = false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("drawer", drawerOpen)
        outState.putString("input", selectedInput)
        outState.putInt("app", grid?.selectedItemPosition ?: appSelection)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        inputManager?.unregisterCallback(callback)
        unregisterReceiver(packages)
        if (Build.VERSION.SDK_INT >= 33) {
            onBackInvokedDispatcher.unregisterOnBackInvokedCallback(backCallback as OnBackInvokedCallback)
        }
        super.onDestroy()
    }

    private fun scheduleRefresh() {
        handler.removeCallbacks(refresh)
        handler.postDelayed(refresh, 250)
    }

    private fun loadHome() {
        try {
            renderHome(InputRepository.read(this, inputManager), null)
        } catch (error: SecurityException) {
            showInputError(error)
        } catch (error: IllegalStateException) {
            showInputError(error)
        }
    }

    private fun showInputError(error: RuntimeException) {
        Log.w("QuietTV", "TV inputs unavailable", error)
        renderHome(emptyList(), "このテレビでは入力情報を取得できません。実機の確認が必要です。")
    }

    private fun createRoot(): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(42), dp(24), dp(42), dp(24))
        }
        applyThemeToRoot(root)
        setContentView(root)
        currentRoot = root
        return root
    }

    // Internal for instrumentation using deterministic HDMI fixtures.
    internal fun renderHome(entries: List<InputEntry>, error: String?) {
        drawerOpen = false
        grid = null
        // Re-read settings on every return, but retain the view tree when unchanged.
        val retainedRoot = homeRoot
        if (retainedRoot != null && entries == homeEntries && error == homeError) {
            if (currentRoot !== retainedRoot) {
                setContentView(retainedRoot)
                currentRoot = retainedRoot
            }
            retainedRoot.findViewWithTag<View>(selectedInput)?.let { preferred ->
                if (!preferred.hasFocus()) preferred.requestFocus()
            }
            return
        }

        val root = createRoot()
        homeRoot = root
        homeEntries = entries.toList()
        homeError = error
        homeMessage = null
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipChildren = false
        }
        val content = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            orientation = LinearLayout.VERTICAL
            clipChildren = false
        }
        scroll.addView(content, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        root.addView(scroll, LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        var preferred: View? = null
        val total = entries.size + 1
        val columns = minOf(4, total)
        // Four cards fit three HDMI ports and All Apps on a single TV row.
        for (rowStart in 0 until total step columns) {
            val row = LinearLayout(this).apply {
                gravity = Gravity.CENTER
                clipChildren = false
            }
            content.addView(row, LinearLayout.LayoutParams(MATCH_PARENT, dp(202)))
            for (index in rowStart until rowStart + columns) {
                if (index >= total) {
                    // Keep incomplete final rows the same width per card.
                    row.addView(View(this), homeTileLayoutParams())
                    continue
                }
                val entry = entries.getOrNull(index)
                val key = entry?.id ?: "apps"
                val tile = TileView(
                    this, entry?.label ?: "すべてのアプリ", null,
                    apps = entry == null, compact = false, theme = theme,
                ).apply {
                    tag = key
                    setOnClickListener {
                        selectedInput = key
                        if (entry == null) showApps() else openInput(entry)
                    }
                    setOnFocusChangeListener { view, hasFocus ->
                        view.invalidate()
                        if (hasFocus) selectedInput = key
                    }
                }
                row.addView(tile, homeTileLayoutParams())
                if (preferred == null || key == selectedInput) preferred = tile
            }
        }
        if (entries.isEmpty()) {
            val message = TextView(this).apply {
                text = error ?: "表示できるHDMI入力がありません。Google TVの入力設定をご確認ください。"
                setTextColor(theme.muted)
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(20), dp(12), 0)
            }
            homeMessage = message
            content.addView(message)
        }
        preferred?.requestFocus()
    }

    private fun homeTileLayoutParams() = LinearLayout.LayoutParams(0, dp(188), 1f).apply {
        setMargins(dp(5), 0, dp(5), 0)
    }

    private fun openInput(entry: InputEntry) {
        val intent = Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(entry.id))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        launchActivity(intent, "入力を開けません。テレビの入力切替ボタンをご利用ください。", Toast.LENGTH_LONG) {
            Log.w("QuietTV", "Cannot open input ${entry.id}", it)
        }
    }

    internal fun showApps() {
        drawerOpen = true
        val root = createRoot().apply { gravity = Gravity.TOP }
        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val title = TextView(this).apply {
            text = "すべてのアプリ"
            textSize = 25f
            setTextColor(theme.foreground)
            setPadding(dp(7), 0, 0, dp(14))
        }
        header.addView(title, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        val settingsButton = Button(this).apply {
            text = "Google TVの設定"
            textSize = 14f
            isAllCaps = false
            tag = "tv-settings"
            id = View.generateViewId()
            setOnClickListener {
                settingsFocused = true
                launchActivity(Intent(Settings.ACTION_SETTINGS), "テレビの設定を開けません。")
            }
        }
        applyThemeToChildren(settingsButton)
        header.addView(settingsButton, LinearLayout.LayoutParams(dp(200), dp(48)))
        root.addView(header, LinearLayout.LayoutParams(MATCH_PARENT, dp(60)))

        val apps = listApps()
        val appGrid = GridView(this).apply {
            id = View.generateViewId()
            nextFocusUpId = settingsButton.id
            setOnFocusChangeListener { _, focused -> if (focused) settingsFocused = false }
            numColumns = 5
            horizontalSpacing = dp(8)
            verticalSpacing = dp(8)
            clipToPadding = false
            setPadding(dp(3), dp(3), dp(3), dp(8))
            setSelector(android.R.color.transparent)
            setDrawSelectorOnTop(false)
            adapter = AppAdapter(apps)
        }
        grid = appGrid
        settingsButton.nextFocusDownId = appGrid.id
        appGrid.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                appSelection = position
                for (index in 0 until appGrid.childCount) {
                    val child = appGrid.getChildAt(index)
                    child.isSelected = child === view
                    child.invalidate()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) = Unit
        }
        appGrid.setOnItemClickListener { _, _, position, _ ->
            appSelection = position
            launchActivity(apps[position].intent, "このアプリは起動できません。")
        }
        root.addView(appGrid, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        appGrid.setSelection(appSelection.coerceAtMost(apps.lastIndex).coerceAtLeast(0))
        if (settingsFocused) settingsButton.requestFocus() else appGrid.requestFocus()
    }

    private inner class AppAdapter(private val apps: List<AppEntry>) : BaseAdapter() {
        override fun getCount(): Int = apps.size
        override fun getItem(position: Int): AppEntry = apps[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val app = apps[position]
            val icon = try {
                app.info.loadIcon(packageManager)
            } catch (error: RuntimeException) {
                Log.w("QuietTV", "App icon unavailable", error)
                null
            }
            return TileView(this@MainActivity, app.label, icon, apps = icon == null, compact = true, theme = theme).apply {
                // GridView owns D-pad focus and selection; children mirror its selection.
                isFocusable = false
                isClickable = false
            }
        }
    }

    private fun listApps(): List<AppEntry> {
        val manager = packageManager
        val byPackage = linkedMapOf<String, AppEntry>()
        // TV entry points win over phone entry points for apps exposing both.
        for (category in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            val query = Intent(Intent.ACTION_MAIN).addCategory(category)
            for (info in manager.queryIntentActivities(query, 0)) {
                val name = info.activityInfo.packageName
                if (name == packageName || name in byPackage) continue
                val launch = Intent(Intent.ACTION_MAIN).addCategory(category)
                    .setComponent(ComponentName(name, info.activityInfo.name))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                byPackage[name] = AppEntry(info.loadLabel(manager).toString(), info, launch)
            }
        }
        val settings = Intent(Settings.ACTION_SETTINGS)
        val systemSettings = manager.resolveActivity(settings, 0)
        if (systemSettings != null && systemSettings.activityInfo.packageName !in byPackage) {
            byPackage[systemSettings.activityInfo.packageName] = AppEntry("設定", systemSettings, settings)
        }
        val collator = Collator.getInstance()
        return byPackage.values.sortedWith { first, second -> collator.compare(first.label, second.label) }
    }

    private fun launchActivity(
        intent: Intent,
        errorMessage: String,
        toastDuration: Int = Toast.LENGTH_SHORT,
        onError: (RuntimeException) -> Unit = {},
    ) {
        try {
            startActivity(intent)
        } catch (error: ActivityNotFoundException) {
            onError(error)
            Toast.makeText(this, errorMessage, toastDuration).show()
        } catch (error: SecurityException) {
            onError(error)
            Toast.makeText(this, errorMessage, toastDuration).show()
        }
    }

    @SuppressLint("GestureBackNavigation") // API <=32 fallback; API 33+ registers OnBackInvokedCallback.
    override fun onBackPressed() = returnHome()

    private fun returnHome() {
        if (drawerOpen) {
            selectedInput = "apps"
            loadHome()
        }
        // Stay on the home surface when this activity is the selected launcher.
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    private data class AppEntry(val label: String, val info: ResolveInfo, val intent: Intent)

    private companion object {
        const val MATCH_PARENT = ViewGroup.LayoutParams.MATCH_PARENT
        const val WRAP_CONTENT = ViewGroup.LayoutParams.WRAP_CONTENT
    }
}
