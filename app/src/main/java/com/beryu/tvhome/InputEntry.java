package com.beryu.tvhome;

import java.util.Objects;

final class InputEntry {
    final String id;
    final String label;
    final InputRules.Brand brand;

    InputEntry(String id, String label) {
        this.id = id;
        this.label = label;
        this.brand = InputRules.brand(label);
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof InputEntry)) return false;
        InputEntry entry = (InputEntry) other;
        return Objects.equals(id, entry.id) && Objects.equals(label, entry.label);
    }

    @Override public int hashCode() { return Objects.hash(id, label); }
}
