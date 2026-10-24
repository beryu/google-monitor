package com.beryu.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.View;

/** Native vector drawing: no bitmap textures, WebView or animation loop. */
@android.annotation.SuppressLint("ViewConstructor") // Created only in Java; never inflated from XML.
final class TileView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint text = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final String label;
    private final Drawable icon;
    private final boolean apps;
    private final boolean compact;
    private final RectF rect = new RectF();
    private ThemePalette theme;

    TileView(Context context, String label, Drawable icon, boolean apps, boolean compact, ThemePalette theme) {
        super(context);
        this.label = label;
        this.icon = icon;
        this.apps = apps;
        this.compact = compact;
        this.theme = theme;
        setFocusable(true);
        setClickable(true);
        setContentDescription(label);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        setOnFocusChangeListener((v, focused) -> invalidate());
    }

    private float dp(float value) { return value * getResources().getDisplayMetrics().density; }

    void applyTheme(ThemePalette theme) {
        this.theme = theme;
        invalidate();
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthSpec), resolveSize((int) dp(compact ? 116 : 188), heightSpec));
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth(), h = getHeight();
        boolean active = hasFocus() || isSelected();
        InputRules.Brand brand = !compact && !apps ? InputRules.brand(label) : InputRules.Brand.NONE;
        int foreground = brand == InputRules.Brand.NINTENDO ? theme.nintendo
                : brand == InputRules.Brand.PLAYSTATION ? theme.playstation
                : theme.foreground;
        float inset = dp(7), radius = dp(compact ? 14 : 21);
        rect.set(inset, inset, w - inset, h - inset);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(active ? theme.focusedCard : theme.card);
        paint.setShadowLayer(dp(active ? 6 : 2), 0, dp(2), active ? theme.focusedShadow : theme.shadow);
        canvas.drawRoundRect(rect, radius, radius, paint);
        paint.clearShadowLayer();
        if (active) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2));
            paint.setColor(theme.focusRing);
            canvas.drawRoundRect(rect, radius, radius, paint);
            paint.setStyle(Paint.Style.FILL);
        }
        float centerY = h * (compact ? .40f : .42f);
        if (icon != null) {
            int size = (int) dp(46);
            icon.setBounds((int)(w / 2 - size / 2f), (int)(centerY - size / 2f),
                    (int)(w / 2 + size / 2f), (int)(centerY + size / 2f));
            icon.draw(canvas);
        } else if (apps) {
            paint.setColor(theme.icon);
            float cell = dp(9), gap = dp(5), total = cell * 3 + gap * 2;
            for (int y = 0; y < 3; y++) for (int x = 0; x < 3; x++) {
                float left = w / 2 - total / 2 + x * (cell + gap);
                float top = centerY - total / 2 + y * (cell + gap);
                canvas.drawRoundRect(left, top, left + cell, top + cell, dp(2), dp(2), paint);
            }
        }
        text.setTypeface(Typeface.create("sans-serif", active ? Typeface.BOLD : Typeface.NORMAL));
        text.setColor(foreground);
        text.setTextSize(dp(compact ? 13 : apps ? 17 : 24));
        text.setTextAlign(Paint.Align.CENTER);
        String fit = TextUtils.ellipsize(label, text, w - dp(38), TextUtils.TruncateAt.END).toString();
        float baseline = !compact && !apps
                ? h / 2 - (text.ascent() + text.descent()) / 2
                : h * (compact ? .83f : .79f);
        canvas.drawText(fit, w / 2, baseline, text);
    }

}
