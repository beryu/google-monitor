package com.beryu.tvhome;

public final class HomeKeyRulesTest {
    public static void main(String[] args) {
        HomeKeyRules keys = new HomeKeyRules();
        check(keys.onKey(24, 0, 0) == HomeKeyRules.Decision.PASS); // Volume up
        check(keys.onKey(83, 0, 0) == HomeKeyRules.Decision.PASS); // Dashboard
        check(keys.onKey(176, 1, 0) == HomeKeyRules.Decision.PASS); // Settings
        check(keys.onKey(3, 1, 0) == HomeKeyRules.Decision.PASS); // No captured down
        check(keys.onKey(3, 0, 2) == HomeKeyRules.Decision.PASS); // Orphan repeat
        check(keys.onKey(3, 0, 0) == HomeKeyRules.Decision.OPEN_HOME);
        check(keys.onKey(3, 0, 1) == HomeKeyRules.Decision.CONSUME);
        check(keys.onKey(3, 0, 10) == HomeKeyRules.Decision.CONSUME);
        check(keys.onKey(25, 0, 0) == HomeKeyRules.Decision.PASS); // Other keys while Home held
        check(keys.onKey(3, 1, 0) == HomeKeyRules.Decision.CONSUME);
        check(keys.onKey(3, 1, 0) == HomeKeyRules.Decision.PASS);
        check(keys.onKey(3, 0, 0) == HomeKeyRules.Decision.OPEN_HOME);
        keys.reset(); // Launch failed or service interrupted
        check(keys.onKey(3, 1, 0) == HomeKeyRules.Decision.PASS);
        check(keys.onKey(3, 2, 0) == HomeKeyRules.Decision.PASS);
        System.out.println("14 direct Home key sequence checks passed.");
    }

    private static void check(boolean value) {
        if (!value) throw new AssertionError("Home key rule failed");
    }
}
