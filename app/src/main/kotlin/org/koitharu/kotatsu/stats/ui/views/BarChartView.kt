package org.koitharu.kotatsu.stats.ui.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.PathEffect
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Xfermode
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt
import androidx.collection.MutableIntList
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.minus
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.setPadding
import com.google.android.material.color.MaterialColors
import org.koitharu.kotatsu.core.util.ext.getThemeColor
import org.koitharu.kotatsu.core.util.ext.resolveDp
import org.koitharu.kotatsu.parsers.util.replaceWith
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.sqrt
import com.google.android.material.R as materialR

class BarChartView @JvmOverloads constructor(
	context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

	private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
	private val bars = ArrayList<Bar>()
	private var maxValue: Int = 0
	private var spacing = context.resources.resolveDp(2f)
	private val minSpace = context.resources.resolveDp(20f)
	private val outlineColor = context.getThemeColor(materialR.attr.colorOutline)
	private val dottedEffect = DashPathEffect(
		floatArrayOf(
			context.resources.resolveDp(6f),
			context.resources.resolveDp(6f),
		),
		0f,
	)
	private val chartBounds = RectF()

	@ColorInt
	var barColor: Int = Color.MAGENTA
		set(value) {
			field = value
			invalidate()
		}

	init {
		paint.strokeWidth = context.resources.resolveDp(1f)
	}

	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)
		if (bars.isEmpty() || chartBounds.isEmpty) {
			return
		}
		val barWidth = ((chartBounds.width() + spacing) / bars.size.toFloat() - spacing)
		// dashed horizontal lines
		paint.color = outlineColor
		paint.style = Paint.Style.STROKE
		canvas.drawLine(chartBounds.left, chartBounds.bottom, chartBounds.right, chartBounds.bottom, paint)
		paint.pathEffect = dottedEffect
		for (i in (0..maxValue).step(computeValueStep())) {
			val y = chartBounds.top + (chartBounds.height() * i / maxValue.toFloat())
			canvas.drawLine(paddingLeft.toFloat(), y, (width - paddingLeft - paddingRight).toFloat(), y, paint)
		}
		// bars
		paint.style = Paint.Style.FILL
		paint.color = barColor
		paint.pathEffect = null
		for ((i, bar) in bars.withIndex()) {
			val h = chartBounds.height() * bar.value / maxValue.toFloat()
			val x = i * (barWidth + spacing) + paddingLeft
			canvas.drawRect(x, chartBounds.bottom - h, x + barWidth, chartBounds.bottom, paint)
		}
		// bottom line
		paint.color = outlineColor
		paint.style = Paint.Style.STROKE
		canvas.drawLine(chartBounds.left, chartBounds.bottom, chartBounds.right, chartBounds.bottom, paint)
	}

	override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
		super.onLayout(changed, left, top, right, bottom)
		invalidateBounds()
	}

	fun setData(value: List<Bar>) {
		bars.replaceWith(value)
		maxValue = if (value.isEmpty()) 0 else value.maxOf { it.value }
		invalidate()
	}

	private fun computeValueStep(): Int {
		val h = chartBounds.height()
		var step = 1
		while (h / (maxValue / step).toFloat() <= minSpace) {
			step++
		}
		return step
	}

	private fun invalidateBounds() {
		val inset = paint.strokeWidth
		chartBounds.set(
			paddingLeft.toFloat() + inset,
			paddingTop.toFloat() + inset,
			(width - paddingLeft - paddingRight).toFloat() - inset,
			(height - paddingTop - paddingBottom).toFloat() - inset,
		)
	}

	class Bar(
		val value: Int,
		val label: String,
	)
}
