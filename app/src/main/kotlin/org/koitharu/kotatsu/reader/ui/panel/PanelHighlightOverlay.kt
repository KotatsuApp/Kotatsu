package org.koitharu.kotatsu.reader.ui.panel

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.max
import kotlin.math.roundToInt
import org.koitharu.kotatsu.reader.domain.panel.PanelPreferences

class PanelHighlightOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val highlightRect = RectF()
    private var hasHighlight = false
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val cornerRadius = resources.displayMetrics.density * 8f

    init {
        applyBorderOpacity(DEFAULT_BORDER_OPACITY)
        updateStrokeWidth()
    }

    fun applyPreferences(preferences: PanelPreferences) {
        applyBorderOpacity(preferences.borderOpacity)
        updateStrokeWidth(preferences.inlineFrames)
        invalidate()
    }

    fun show(rect: RectF) {
        highlightRect.set(rect)
        hasHighlight = true
        if (visibility != VISIBLE) {
            visibility = VISIBLE
        }
        invalidate()
    }

    fun hide() {
        hasHighlight = false
        if (visibility != GONE) {
            visibility = GONE
        } else {
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (!hasHighlight) {
            return
        }
        canvas.drawRoundRect(highlightRect, cornerRadius, cornerRadius, fillPaint)
        canvas.drawRoundRect(highlightRect, cornerRadius, cornerRadius, strokePaint)
    }

    private fun applyBorderOpacity(opacity: Float) {
        val clamped = opacity.coerceIn(0f, 1f)
        val strokeAlpha = (clamped * 255f).roundToInt().coerceIn(0, 255)
        strokePaint.color = Color.argb(strokeAlpha, 255, 255, 255)
        val fillAlpha = (strokeAlpha * FILL_ALPHA_MULTIPLIER).roundToInt().coerceIn(0, 255)
        fillPaint.color = Color.argb(fillAlpha, 255, 255, 255)
    }

    private fun updateStrokeWidth(inlineFrames: Boolean = false) {
        val density = resources.displayMetrics.density
        val desired = if (inlineFrames) INLINE_STROKE_DP else DEFAULT_STROKE_DP
        strokePaint.strokeWidth = max(1f, density * desired)
    }

    private companion object {
        private const val DEFAULT_BORDER_OPACITY = 0.5f
        private const val DEFAULT_STROKE_DP = 2f
        private const val INLINE_STROKE_DP = 1.25f
        private const val FILL_ALPHA_MULTIPLIER = 0.25f
    }
}
