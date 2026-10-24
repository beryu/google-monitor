package com.beryu.tvhome

internal data class InputEntry(val id: String, val label: String) {
    val brand: InputRules.Brand = InputRules.brand(label)
}
