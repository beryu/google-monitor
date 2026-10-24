package com.beryu.tvhome;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.tv.TvContract;
import android.media.tv.TvInputManager;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.text.Collator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {
    private TvInputManager inputManager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean drawerOpen;
    private String selectedInput = "";
    private int appSelection;
    private GridView grid;
    private Object backCallback;
    private boolean settingsFocused;
    private final Runnable refresh = () -> { if (!drawerOpen) loadHome(); };
    private final TvInputManager.TvInputCallback callback = new TvInputManager.TvInputCallback() {
        @Override public void onInputAdded(String id) { scheduleRefresh(); }
        @Override public void onInputRemoved(String id) { scheduleRefresh(); }
        @Override public void onInputUpdated(String id) { scheduleRefresh(); }
        @Override public void onInputStateChanged(String id, int state) { scheduleRefresh(); }
    };
    private final BroadcastReceiver packages = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent intent) { if (drawerOpen) showApps(); }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (state != null) {
            drawerOpen = state.getBoolean("drawer");
            selectedInput = state.getString("input", "");
            appSelection = state.getInt("app");
        }
        inputManager = (TvInputManager) getSystemService(TV_INPUT_SERVICE);
        if (inputManager != null) inputManager.registerCallback(callback, handler);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addDataScheme("package");
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(packages, filter, Context.RECEIVER_NOT_EXPORTED);
            backCallback = (android.window.OnBackInvokedCallback) this::returnHome;
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                    (android.window.OnBackInvokedCallback) backCallback);
        } else {
            registerReceiver(packages, filter);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        if (drawerOpen) showApps(); else loadHome();
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        // The Home key always returns to the minimal home, including from the drawer.
        drawerOpen = false;
        loadHome();
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putBoolean("drawer", drawerOpen);
        out.putString("input", selectedInput);
        out.putInt("app", grid == null ? appSelection : grid.getSelectedItemPosition());
    }

    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (inputManager != null) inputManager.unregisterCallback(callback);
        unregisterReceiver(packages);
        if (Build.VERSION.SDK_INT >= 33) {
            getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(
                    (android.window.OnBackInvokedCallback) backCallback);
        }
        super.onDestroy();
    }

    private void scheduleRefresh() {
        handler.removeCallbacks(refresh);
        handler.postDelayed(refresh, 250);
    }

    private void loadHome() {
        try { renderHome(InputRepository.read(this, inputManager), null); }
        catch (SecurityException | IllegalStateException e) {
            Log.w("QuietTV", "TV inputs unavailable", e);
            renderHome(new ArrayList<>(), "このテレビでは入力情報を取得できません。実機の確認が必要です。");
        }
    }

    private LinearLayout root() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(42), dp(24), dp(42), dp(24));
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFFF4F2ED, 0xFFE5E8E8}));
        setContentView(root);
        return root;
    }

    // Package visible for instrumentation using deterministic HDMI fixtures.
    void renderHome(List<InputEntry> entries, String error) {
        drawerOpen = false;
        grid = null;
        LinearLayout root = root();
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipChildren(false);
        LinearLayout content = new LinearLayout(this);
        content.setGravity(Gravity.CENTER);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setClipChildren(false);
        scroll.addView(content, new ScrollView.LayoutParams(-1, -1));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, -1));

        View preferred = null;
        LinearLayout row = null;
        int total = entries.size() + 1;
        int columns = Math.min(4, total);
        // Four cards fit three HDMI ports and All Apps on a single TV row.
        for (int i = 0; i < total; i++) {
            if (i % columns == 0) {
                row = new LinearLayout(this);
                row.setGravity(Gravity.CENTER);
                row.setClipChildren(false);
                content.addView(row, new LinearLayout.LayoutParams(-1, dp(202)));
            }
            final InputEntry entry = i < entries.size() ? entries.get(i) : null;
            TileView tile = new TileView(this, entry == null ? "すべてのアプリ" : entry.label,
                    null, entry == null, false);
            String key = entry == null ? "apps" : entry.id;
            tile.setTag(key);
            tile.setOnClickListener(v -> {
                selectedInput = key;
                if (entry == null) showApps(); else openInput(entry);
            });
            tile.setOnFocusChangeListener((v, hasFocus) -> {
                v.invalidate();
                if (hasFocus) selectedInput = key;
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(188), 1);
            lp.setMargins(dp(5), 0, dp(5), 0);
            row.addView(tile, lp);
            if (preferred == null || key.equals(selectedInput)) preferred = tile;
        }
        // Keep incomplete final rows the same width per card.
        for (int i = total; i % columns != 0; i++) {
            View spacer = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(188), 1);
            lp.setMargins(dp(5), 0, dp(5), 0);
            row.addView(spacer, lp);
        }
        if (entries.isEmpty()) {
            TextView message = new TextView(this);
            message.setText(error != null ? error : "表示できるHDMI入力がありません。Google TVの入力設定をご確認ください。");
            message.setTextColor(0xFF626B70);
            message.setTextSize(14);
            message.setGravity(Gravity.CENTER);
            message.setPadding(dp(12), dp(20), dp(12), 0);
            content.addView(message);
        }
        if (preferred != null) preferred.requestFocus();
    }

    private void openInput(InputEntry entry) {
        Intent intent = new Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(entry.id));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try { startActivity(intent); }
        catch (ActivityNotFoundException | SecurityException e) {
            Log.w("QuietTV", "Cannot open input " + entry.id, e);
            Toast.makeText(this, "入力を開けません。テレビの入力切替ボタンをご利用ください。", Toast.LENGTH_LONG).show();
        }
    }

    void showApps() {
        drawerOpen = true;
        LinearLayout root = root();
        root.setGravity(Gravity.TOP);
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(this);
        title.setText("すべてのアプリ");
        title.setTextSize(25);
        title.setTextColor(0xFF29333B);
        title.setPadding(dp(7), 0, 0, dp(14));
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        Button settingsButton = new Button(this);
        settingsButton.setText("Google TVの設定");
        settingsButton.setTextSize(14);
        settingsButton.setAllCaps(false);
        settingsButton.setTag("tv-settings");
        settingsButton.setId(View.generateViewId());
        settingsButton.setOnClickListener(v -> {
            settingsFocused = true;
            try { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
            catch (ActivityNotFoundException | SecurityException e) {
                Toast.makeText(this, "テレビの設定を開けません。", Toast.LENGTH_SHORT).show();
            }
        });
        header.addView(settingsButton, new LinearLayout.LayoutParams(dp(200), dp(48)));
        root.addView(header, new LinearLayout.LayoutParams(-1, dp(60)));
        final List<AppEntry> apps = listApps();
        grid = new GridView(this);
        grid.setId(View.generateViewId());
        grid.setNextFocusUpId(settingsButton.getId());
        settingsButton.setNextFocusDownId(grid.getId());
        grid.setOnFocusChangeListener((v, focused) -> { if (focused) settingsFocused = false; });
        grid.setNumColumns(5);
        grid.setHorizontalSpacing(dp(8));
        grid.setVerticalSpacing(dp(8));
        grid.setClipToPadding(false);
        grid.setPadding(dp(3), dp(3), dp(3), dp(8));
        grid.setSelector(android.R.color.transparent);
        grid.setDrawSelectorOnTop(false);
        grid.setAdapter(new BaseAdapter() {
            @Override public int getCount() { return apps.size(); }
            @Override public Object getItem(int position) { return apps.get(position); }
            @Override public long getItemId(int position) { return position; }
            @Override public View getView(int position, View convert, ViewGroup parent) {
                AppEntry app = apps.get(position);
                android.graphics.drawable.Drawable drawable = null;
                try { drawable = app.info.loadIcon(getPackageManager()); }
                catch (RuntimeException e) { Log.w("QuietTV", "App icon unavailable", e); }
                TileView tile = new TileView(MainActivity.this, app.label, drawable, drawable == null, true);
                // GridView owns D-pad focus and selection; children mirror its selection.
                tile.setFocusable(false);
                tile.setClickable(false);
                return tile;
            }
        });
        grid.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                appSelection = position;
                for (int i = 0; i < grid.getChildCount(); i++) {
                    View child = grid.getChildAt(i);
                    child.setSelected(child == view);
                    child.invalidate();
                }
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        grid.setOnItemClickListener((parent, view, position, id) -> {
            appSelection = position;
            try { startActivity(apps.get(position).intent); }
            catch (ActivityNotFoundException | SecurityException e) {
                Toast.makeText(this, "このアプリは起動できません。", Toast.LENGTH_SHORT).show();
            }
        });
        root.addView(grid, new LinearLayout.LayoutParams(-1, 0, 1));
        grid.setSelection(Math.max(0, Math.min(appSelection, apps.size() - 1)));
        if (settingsFocused) settingsButton.requestFocus(); else grid.requestFocus();
    }

    private List<AppEntry> listApps() {
        PackageManager pm = getPackageManager();
        Map<String, AppEntry> byPackage = new LinkedHashMap<>();
        // TV entry points win over phone entry points for apps exposing both.
        for (String category : new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
            Intent query = new Intent(Intent.ACTION_MAIN).addCategory(category);
            for (ResolveInfo info : pm.queryIntentActivities(query, 0)) {
                String pkg = info.activityInfo.packageName;
                if (pkg.equals(getPackageName()) || byPackage.containsKey(pkg)) continue;
                Intent launch = new Intent(Intent.ACTION_MAIN).addCategory(category)
                        .setComponent(new ComponentName(pkg, info.activityInfo.name))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                byPackage.put(pkg, new AppEntry(info.loadLabel(pm).toString(), info, launch));
            }
        }
        Intent settings = new Intent(Settings.ACTION_SETTINGS);
        ResolveInfo systemSettings = pm.resolveActivity(settings, 0);
        if (systemSettings != null && !byPackage.containsKey(systemSettings.activityInfo.packageName)) {
            byPackage.put(systemSettings.activityInfo.packageName, new AppEntry("設定", systemSettings, settings));
        }
        List<AppEntry> apps = new ArrayList<>(byPackage.values());
        Collator collator = Collator.getInstance();
        apps.sort((a, b) -> collator.compare(a.label, b.label));
        return apps;
    }

    @android.annotation.SuppressLint("GestureBackNavigation") // API <=32 fallback; API 33+ registers OnBackInvokedCallback.
    @Override public void onBackPressed() {
        returnHome();
    }

    private void returnHome() {
        if (drawerOpen) { selectedInput = "apps"; loadHome(); }
        // Stay on the home surface when this activity is the selected launcher.
    }

    private int dp(float n) { return Math.round(n * getResources().getDisplayMetrics().density); }

    private static final class AppEntry {
        final String label;
        final ResolveInfo info;
        final Intent intent;
        AppEntry(String label, ResolveInfo info, Intent intent) {
            this.label = label; this.info = info; this.intent = intent;
        }
    }
}
