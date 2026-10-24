package com.beryu.tvhome;

/** Colors shared by the home, app drawer, and custom-drawn cards. */
final class ThemePalette {
    static final ThemePalette LIGHT = new ThemePalette(false);
    static final ThemePalette DARK = new ThemePalette(true);

    final int backgroundStart, backgroundEnd, foreground, muted, card, focusedCard;
    final int focusRing, icon, shadow, focusedShadow, button, focusedButton;
    final int nintendo, playstation;

    private ThemePalette(boolean dark) {
        backgroundStart = dark ? 0xFF151B22 : 0xFFF4F2ED;
        backgroundEnd = dark ? 0xFF202A33 : 0xFFE5E8E8;
        foreground = dark ? 0xFFE8EDF2 : 0xFF29333B;
        muted = dark ? 0xFFB4BFC9 : 0xFF626B70;
        card = dark ? 0xFF28323D : 0xFFF7F7F4;
        focusedCard = dark ? 0xFF364554 : 0xFFFFFFFF;
        focusRing = dark ? 0xFFB5D4EB : 0xFF566B7A;
        icon = dark ? 0xFFC6D3DE : 0xFF44515B;
        shadow = dark ? 0x40000000 : 0x10303A43;
        focusedShadow = dark ? 0x66000000 : 0x22303A43;
        button = dark ? 0xFF28323D : 0xFFE0E4E6;
        focusedButton = dark ? 0xFF526B80 : 0xFFC1CED7;
        nintendo = dark ? 0xFFFF7B86 : 0xFFE60012;
        playstation = dark ? 0xFF79BFFF : 0xFF0070CC;
    }
}
