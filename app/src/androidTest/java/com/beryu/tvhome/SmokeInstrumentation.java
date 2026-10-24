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
    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); start(); }
    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            MainActivity activity = (MainActivity) startActivitySync(new Intent(getTargetContext(), MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            waitForIdleSync();
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
            // Return to the real input list after the fixtures.
            runOnMainSync(() -> activity.showApps());
            sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
            waitForIdleSync();
            // Leave the real home visible after checking fixtures.
            result.putString("stream", "PASS: home focus, D-pad movement, app drawer, settings, generic HDMI, four-card row, wrapping, Back\n");
            finish(-1, result);
        } catch (Throwable e) {
            result.putString("stream", "FAIL: " + e + "\n");
            finish(0, result);
        }
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
