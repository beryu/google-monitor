package com.beryu.tvhome

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.TextPaint
import android.text.TextUtils
import android.view.View

/** Native vector drawing: no bitmap textures, WebView or animation loop. */
@SuppressLint("ViewConstructor") // Created only in Kotlin; never inflated from XML.
internal class TileView(
    context: Context,
    private val label: String,
    private val icon: Drawable?,
    private val apps: Boolean,
    private val compact: Boolean,
    private var theme: ThemePalette,
) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    init {
        isFocusable = true
        isClickable = true
        contentDescription = label
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        setOnFocusChangeListener { _, _ -> invalidate() }
    }

    fun applyTheme(theme: ThemePalette) {
        this.theme = theme
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            MeasureSpec.getSize(widthMeasureSpec),
            resolveSize(dp(if (compact) 116f else 188f).toInt(), heightMeasureSpec),
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val width = width.toFloat()
        val height = height.toFloat()
        val active = hasFocus() || isSelected
        val brand = if (!compact && !apps) InputRules.brand(label) else InputRules.Brand.NONE
        val foreground = when (brand) {
            InputRules.Brand.NINTENDO -> theme.nintendo
            InputRules.Brand.PLAYSTATION -> theme.playstation
            InputRules.Brand.NONE -> theme.foreground
        }
        val inset = dp(7f)
        val radius = dp(if (compact) 14f else 21f)
        rect.set(inset, inset, width - inset, height - inset)
        paint.style = Paint.Style.FILL
        paint.color = if (active) theme.focusedCard else theme.card
        paint.setShadowLayer(
            dp(if (active) 6f else 2f), 0f, dp(2f),
            if (active) theme.focusedShadow else theme.shadow,
        )
        canvas.drawRoundRect(rect, radius, radius, paint)
        paint.clearShadowLayer()
        if (active) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = dp(2f)
            paint.color = theme.focusRing
            canvas.drawRoundRect(rect, radius, radius, paint)
            paint.style = Paint.Style.FILL
        }

        val centerY = height * if (compact) .40f else .42f
        if (icon != null) {
            val size = dp(46f).toInt()
            icon.setBounds(
                (width / 2 - size / 2f).toInt(), (centerY - size / 2f).toInt(),
                (width / 2 + size / 2f).toInt(), (centerY + size / 2f).toInt(),
            )
            icon.draw(canvas)
        } else if (apps) {
            drawAppsIcon(canvas, width / 2, centerY)
        }

        textPaint.typeface = Typeface.create("sans-serif", if (active) Typeface.BOLD else Typeface.NORMAL)
        textPaint.color = foreground
        textPaint.textSize = dp(when {
            compact -> 13f
            apps -> 17f
            else -> 24f
        })
        textPaint.textAlign = Paint.Align.CENTER
        val fit = TextUtils.ellipsize(label, textPaint, width - dp(38f), TextUtils.TruncateAt.END)
        val baseline = if (!compact && !apps) {
            height / 2 - (textPaint.ascent() + textPaint.descent()) / 2
        } else {
            height * if (compact) .83f else .79f
        }
        canvas.drawText(fit.toString(), width / 2, baseline, textPaint)
    }

    private fun drawAppsIcon(canvas: Canvas, centerX: Float, centerY: Float) {
        paint.color = theme.icon
        val cell = dp(9f)
        val gap = dp(5f)
        val total = cell * 3 + gap * 2
        for (y in 0..2) {
            for (x in 0..2) {
                val left = centerX - total / 2 + x * (cell + gap)
                val top = centerY - total / 2 + y * (cell + gap)
                canvas.drawRoundRect(left, top, left + cell, top + cell, dp(2f), dp(2f), paint)
            }
        }
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}
