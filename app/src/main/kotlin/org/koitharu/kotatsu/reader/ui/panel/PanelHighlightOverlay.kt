package org.koitharu.kotatsu.reader.ui.panel

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.max

class PanelHighlightOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val highlightRect = RectF()
    private var hasHighlight = false
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = max(2f, resources.displayMetrics.density * 2f)
        color = 0xCCFFFFFF.toInt()
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x33FFFFFF
    }
    private val cornerRadius = resources.displayMetrics.density * 8f

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
}

