package com.beryu.tvhome;

import java.util.Locale;

/** Pure rules kept independent of Android so ambiguous names are testable. */
public final class InputRules {
    public enum Brand { NINTENDO, PLAYSTATION, NONE }
    private InputRules() {}

    public static Brand brand(String label) {
        String normalized = label == null ? "" : label.toLowerCase(Locale.ROOT);
        if (normalized.contains("switch")) return Brand.NINTENDO;
        if (normalized.contains("playstation")) return Brand.PLAYSTATION;
        return Brand.NONE;
    }

    public static String label(CharSequence custom, CharSequence fallback) {
        if (custom != null && !custom.toString().trim().isEmpty()) return custom.toString();
        return fallback == null ? "HDMI" : fallback.toString();
    }
}
