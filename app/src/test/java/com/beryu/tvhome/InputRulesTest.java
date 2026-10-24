package com.beryu.tvhome;

import java.util.Locale;

public final class InputRulesTest {
    public static void main(String[] args) {
        check(InputRules.brand("Nintendo Switch 2") == InputRules.Brand.NINTENDO);
        check(InputRules.brand("HDMI 2 · PlayStation 5") == InputRules.Brand.PLAYSTATION);
        check(InputRules.brand("switch") == InputRules.Brand.NINTENDO);
        check(InputRules.brand("PLAYSTATION") == InputRules.Brand.PLAYSTATION);
        check(InputRules.brand("PS5") == InputRules.Brand.NONE);
        check(InputRules.brand(null) == InputRules.Brand.NONE);
        check(InputRules.brand("PC") == InputRules.Brand.NONE);
        check(InputRules.label("  Switch 2  ", "HDMI 1").equals("  Switch 2  "));
        check(InputRules.label("  ", "HDMI 1").equals("HDMI 1"));
        check(InputRules.label(null, "PlayStation").equals("PlayStation"));
        Locale old = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr", "TR"));
            check(InputRules.brand("SWITCH") == InputRules.Brand.NINTENDO);
        } finally { Locale.setDefault(old); }
        System.out.println("11 input label and matching checks passed.");
    }
    private static void check(boolean value) {
        if (!value) throw new AssertionError("Input rule failed");
    }
}
