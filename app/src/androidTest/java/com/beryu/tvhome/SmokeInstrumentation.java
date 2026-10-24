package com.beryu.tvhome;

import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;

/** Explicitly invoked smoke checks. Fixtures never appear during normal launches. */
public final class SmokeInstrumentation extends Instrumentation {
    private boolean benchmark;
    @Override public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        benchmark = arguments != null && "true".equals(arguments.getString("benchmark"));
        start();
    }
    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            MainActivity activity = (MainActivity) startActivitySync(new Intent(getTargetContext(), MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            waitForIdleSync();
            if (benchmark) {
                benchmarkHomeReturn(activity, result);
                finish(-1, result);
                return;
            }
            runOnMainSync(() -> activity.renderHome(Arrays.asList(
                    new InputEntry("fixture-hdmi-1", "Nintendo Switch 2"),
                    new InputEntry("fixture-hdmi-2", "PlayStation 5")), null));
            waitForIdleSync();
            runOnMainSync(() -> require("fixture-hdmi-1".equals(activity.getCurrentFocus().getTag()), "Initial focus"));
            screenshot("home-preview.png");
            sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT);
            waitForIdleSync();
            runOnMainSync(() -> require("fixture-hdmi-2".equals(activity.getCurrentFocus().getTag()), "Right to PlayStation"));
            sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT);
            waitForIdleSync();
            runOnMainSync(() -> require("apps".equals(activity.getCurrentFocus().getTag()), "Right to apps"));
            sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_CENTER);
            waitForIdleSync();
            runOnMainSync(() -> {
                GridView grid = findGrid(activity.getWindow().getDecorView());
                require(grid != null && grid.getCount() > 0, "App drawer contains launchable apps");
                require(grid.hasFocus(), "Grid has D-pad focus");
                require(grid.getSelectedItemPosition() == 0, "First app selected");
            });
            screenshot("apps-preview.png");
            sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT);
            waitForIdleSync();
            runOnMainSync(() -> {
                GridView grid = findGrid(activity.getWindow().getDecorView());
                if (grid.getCount() > 1) require(grid.getSelectedItemPosition() == 1, "Grid D-pad movement");
            });
            sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_UP);
            waitForIdleSync();
            runOnMainSync(() -> require("tv-settings".equals(activity.getCurrentFocus().getTag()), "Settings reachable with D-pad"));
            ActivityMonitor settings = addMonitor(new android.content.IntentFilter(android.provider.Settings.ACTION_SETTINGS),
                    new android.app.Instrumentation.ActivityResult(0, null), true);
            sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_CENTER);
            waitForIdleSync();
            require(settings.getHits() == 1, "Settings action launched");
            removeMonitor(settings);
            sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
            waitForIdleSync();
            runOnMainSync(() -> require("apps".equals(activity.getCurrentFocus().getTag()), "Back restores apps focus"));
            runOnMainSync(() -> activity.renderHome(Arrays.asList(
                    new InputEntry("fixture-hdmi-1", "Nintendo Switch 2"),
                    new InputEntry("fixture-hdmi-2", "PlayStation 5"),
                    new InputEntry("fixture-hdmi-3", "Aux")), null));
            waitForIdleSync();
            runOnMainSync(() -> {
                View decor = activity.getWindow().getDecorView();
                View first = decor.findViewWithTag("fixture-hdmi-1");
                View generic = decor.findViewWithTag("fixture-hdmi-3");
                View apps = decor.findViewWithTag("apps");
                require(generic != null && generic.isFocusable(), "Generic HDMI input is accessible");
                require(first.getParent() == generic.getParent() && first.getParent() == apps.getParent(),
                        "Three inputs and All Apps fit on one row");
                require(Math.abs(first.getWidth() - apps.getWidth()) <= 1, "Cards have equal width");
                generic.requestFocus();
            });
            sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT);
            waitForIdleSync();
            runOnMainSync(() -> require("apps".equals(activity.getCurrentFocus().getTag()),
                    "Generic HDMI navigates to All Apps"));
            screenshot("four-card-preview.png");
            runOnMainSync(() -> activity.renderHome(Arrays.asList(
                    new InputEntry("fixture-hdmi-1", "HDMI 1"),
                    new InputEntry("fixture-hdmi-2", "HDMI 2"),
                    new InputEntry("fixture-hdmi-3", "HDMI 3"),
                    new InputEntry("fixture-hdmi-4", "HDMI 4")), null));
            waitForIdleSync();
            runOnMainSync(() -> {
                View decor = activity.getWindow().getDecorView();
                View first = decor.findViewWithTag("fixture-hdmi-1");
                View apps = decor.findViewWithTag("apps");
                require(first.getParent() != apps.getParent(), "Additional inputs wrap to another row");
                require(Math.abs(first.getWidth() - apps.getWidth()) <= 1, "Final row keeps card width");
                decor.findViewWithTag("fixture-hdmi-4").requestFocus();
            });
            sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN);
            waitForIdleSync();
            runOnMainSync(() -> require("apps".equals(activity.getCurrentFocus().getTag()),
                    "Wrapped row remains reachable with D-pad"));
            checkHomeReuse(activity);
            // Return to the real input list after the fixtures.
            runOnMainSync(() -> activity.showApps());
            sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
            waitForIdleSync();
            // Leave the real home visible after checking fixtures.
            result.putString("stream", "PASS: home reuse, input changes, Home lifecycle, focus, D-pad movement, app drawer, settings, generic HDMI, four-card row, wrapping, Back\n");
            finish(-1, result);
        } catch (Throwable e) {
            result.putString("stream", "FAIL: " + e + "\n");
            finish(0, result);
        }
    }

    private void checkHomeReuse(MainActivity activity) {
        runOnMainSync(() -> {
            java.util.List<InputEntry> inputs = Arrays.asList(
                    new InputEntry("reuse-1", "Switch"), new InputEntry("reuse-2", "PlayStation"));
            activity.renderHome(inputs, null);
            View tile = activity.getWindow().getDecorView().findViewWithTag("reuse-2");
            tile.requestFocus();
            activity.renderHome(Arrays.asList(new InputEntry("reuse-1", "Switch"),
                    new InputEntry("reuse-2", "PlayStation")), null);
            require(activity.getWindow().getDecorView().findViewWithTag("reuse-2") == tile,
                    "Unchanged inputs reuse cards");
            require(activity.getCurrentFocus() == tile, "Reuse preserves focused input");
            View apps = activity.getWindow().getDecorView().findViewWithTag("apps");
            apps.performClick();
            activity.renderHome(inputs, null);
            require(activity.getWindow().getDecorView().findViewWithTag("apps") == apps,
                    "Drawer return reuses home cards");
            require(activity.getCurrentFocus() == apps, "Drawer return focuses All Apps");
            activity.renderHome(Arrays.asList(new InputEntry("reuse-1", "PC"),
                    new InputEntry("reuse-2", "PlayStation")), null);
            require("PC".contentEquals(activity.getWindow().getDecorView()
                    .findViewWithTag("reuse-1").getContentDescription()), "Renamed input updates");
            activity.renderHome(Arrays.asList(new InputEntry("reuse-1", "PC")), null);
            require(activity.getWindow().getDecorView().findViewWithTag("reuse-2") == null,
                    "Hidden or removed input disappears");
            activity.renderHome(java.util.Collections.emptyList(), "Input error");
            View errorRoot = activity.getWindow().getDecorView().findViewWithTag("apps");
            activity.renderHome(java.util.Collections.emptyList(), null);
            require(activity.getWindow().getDecorView().findViewWithTag("apps") != errorRoot,
                    "Error recovery refreshes home");
        });
        // Exercise Android's real onNewIntent -> onResume delivery as well.
        relaunchHome(activity);
        final View[] apps = new View[1];
        runOnMainSync(() -> {
            apps[0] = activity.getWindow().getDecorView().findViewWithTag("apps");
            apps[0].performClick();
        });
        relaunchHome(activity);
        runOnMainSync(() -> {
            require(findGrid(activity.getWindow().getDecorView()) == null, "Home closes drawer");
            require(activity.getWindow().getDecorView().findViewWithTag("apps") == apps[0],
                    "Home intent restores retained home");
        });
    }

    private void relaunchHome(MainActivity activity) {
        // startActivitySync waits for onCreate and cannot wait for an existing singleTask.
        ActivityMonitor monitor = addMonitor(MainActivity.class.getName(), null, false);
        try {
            runOnMainSync(() -> activity.startActivity(new Intent(activity, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
            require(waitForMonitorWithTimeout(monitor, 5000) == activity, "Home intent resumes same activity");
            waitForIdleSync();
        } finally { removeMonitor(monitor); }
    }

    private void benchmarkHomeReturn(MainActivity activity, Bundle result) {
        // Both APK versions run exactly this sequence. Measures synchronous app work,
        // not physical remote latency, window animation, TV wake or cold startup.
        long[] samples = new long[31];
        int[] replaced = {0};
        for (int i = -5; i < samples.length; i++) {
            final int sample = i;
            runOnMainSync(() -> {
                View apps = activity.getWindow().getDecorView().findViewWithTag("apps");
                long start = android.os.SystemClock.elapsedRealtimeNanos();
                activity.onNewIntent(new Intent(getTargetContext(), MainActivity.class));
                activity.onResume();
                long elapsed = android.os.SystemClock.elapsedRealtimeNanos() - start;
                if (sample >= 0) {
                    samples[sample] = elapsed;
                    if (apps != activity.getWindow().getDecorView().findViewWithTag("apps")) replaced[0]++;
                }
            });
            waitForIdleSync();
        }
        Arrays.sort(samples);
        result.putString("stream", String.format(java.util.Locale.ROOT,
                "Home return app work: median=%.3f ms, p90=%.3f ms, card replacements=%d/31 (5 warmups)\n",
                samples[15] / 1_000_000.0, samples[27] / 1_000_000.0, replaced[0]));
    }
    private void screenshot(String name) throws Exception {
        Bitmap bitmap = getUiAutomation().takeScreenshot();
        require(bitmap != null, "Screenshot available");
        File file = new File(getTargetContext().getFilesDir(), name);
        try (FileOutputStream out = new FileOutputStream(file)) { bitmap.compress(Bitmap.CompressFormat.PNG, 100, out); }
        bitmap.recycle();
    }
    private static GridView findGrid(View view) {
        if (view instanceof GridView) return (GridView) view;
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
            GridView found = findGrid(((ViewGroup) view).getChildAt(i));
            if (found != null) return found;
        }
        return null;
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
