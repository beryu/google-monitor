package com.beryu.tvhome;

final class InputEntry {
    final String id;
    final String label;
    final InputRules.Brand brand;

    InputEntry(String id, String label) {
        this.id = id;
        this.label = label;
        this.brand = InputRules.brand(label);
    }
}
