package com.beryu.tvhome

/** Colors shared by the home, app drawer, and custom-drawn cards. */
internal class ThemePalette private constructor(dark: Boolean) {
    val backgroundStart = if (dark) 0xFF151B22.toInt() else 0xFFF4F2ED.toInt()
    val backgroundEnd = if (dark) 0xFF202A33.toInt() else 0xFFE5E8E8.toInt()
    val foreground = if (dark) 0xFFE8EDF2.toInt() else 0xFF29333B.toInt()
    val muted = if (dark) 0xFFB4BFC9.toInt() else 0xFF626B70.toInt()
    val card = if (dark) 0xFF28323D.toInt() else 0xFFF7F7F4.toInt()
    val focusedCard = if (dark) 0xFF364554.toInt() else 0xFFFFFFFF.toInt()
    val focusRing = if (dark) 0xFFB5D4EB.toInt() else 0xFF566B7A.toInt()
    val icon = if (dark) 0xFFC6D3DE.toInt() else 0xFF44515B.toInt()
    val shadow = if (dark) 0x40000000 else 0x10303A43
    val focusedShadow = if (dark) 0x66000000 else 0x22303A43
    val button = if (dark) 0xFF28323D.toInt() else 0xFFE0E4E6.toInt()
    val focusedButton = if (dark) 0xFF526B80.toInt() else 0xFFC1CED7.toInt()
    val nintendo = if (dark) 0xFFFF7B86.toInt() else 0xFFE60012.toInt()
    val playstation = if (dark) 0xFF79BFFF.toInt() else 0xFF0070CC.toInt()

    companion object {
        val LIGHT = ThemePalette(dark = false)
        val DARK = ThemePalette(dark = true)
    }
}
