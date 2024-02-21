package org.koitharu.kotatsu.stats.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Xfermode
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import androidx.collection.MutableIntList
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.minus
import com.google.android.material.color.MaterialColors
import org.koitharu.kotatsu.core.util.ext.getThemeColor
import org.koitharu.kotatsu.core.util.ext.resolveDp
import org.koitharu.kotatsu.parsers.util.replaceWith
import kotlin.math.absoluteValue
import com.google.android.material.R as materialR

class PieChartView @JvmOverloads constructor(
	context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

	private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
	private val segments = ArrayList<Segment>()
	private val chartBounds = RectF()
	private val clearColor = context.getThemeColor(android.R.attr.colorBackground)

	init {
		paint.strokeWidth = context.resources.resolveDp(2f)
	}

	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)
		var angle = 0f
		for ((i, segment) in segments.withIndex()) {
			paint.color = segment.color
			paint.style = Paint.Style.FILL
			val sweepAngle = segment.percent * 360f
			canvas.drawArc(
				chartBounds,
				angle,
				sweepAngle,
				true,
				paint,
			)
			paint.color = clearColor
			paint.style = Paint.Style.STROKE
			canvas.drawArc(
				chartBounds,
				angle,
				sweepAngle,
				true,
				paint,
			)
			angle += sweepAngle
		}
		paint.style = Paint.Style.FILL
		paint.color = clearColor
		canvas.drawCircle(chartBounds.centerX(), chartBounds.centerY(), chartBounds.height() / 4f, paint)
	}

	override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
		super.onSizeChanged(w, h, oldw, oldh)
		val size = minOf(w, h).toFloat()
		val inset = paint.strokeWidth
		chartBounds.set(inset, inset, size - inset, size - inset)
		chartBounds.offset(
			(w - size) / 2f,
			(h - size) / 2f,
		)
	}

	fun setData(value: List<Segment>) {
		segments.replaceWith(value)
		invalidate()
	}

	class Segment(
		val value: Int,
		val label: String,
		val percent: Float,
		val color: Int,
	)
}
