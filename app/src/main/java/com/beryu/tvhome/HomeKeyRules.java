package com.beryu.tvhome;

/** Keeps Home down/up paired; every other key passes through unchanged. */
public final class HomeKeyRules {
    public enum Decision { PASS, OPEN_HOME, CONSUME }
    private boolean handlingHome;

    public Decision onKey(int keyCode, int action, int repeatCount) {
        // Android's stable KEYCODE_HOME / ACTION_DOWN / ACTION_UP constants.
        if (keyCode != 3) return Decision.PASS;
        if (action == 0) {
            if (repeatCount == 0) {
                handlingHome = true;
                return Decision.OPEN_HOME;
            }
            return handlingHome ? Decision.CONSUME : Decision.PASS;
        }
        if (action == 1) {
            boolean handled = handlingHome;
            handlingHome = false;
            return handled ? Decision.CONSUME : Decision.PASS;
        }
        return Decision.PASS;
    }

    public void reset() { handlingHome = false; }
}
