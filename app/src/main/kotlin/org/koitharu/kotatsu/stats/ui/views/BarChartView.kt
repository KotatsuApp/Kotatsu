package org.koitharu.kotatsu.stats.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import org.koitharu.kotatsu.core.util.ext.getThemeColor
import org.koitharu.kotatsu.core.util.ext.resolveDp
import org.koitharu.kotatsu.parsers.util.replaceWith
import org.koitharu.kotatsu.parsers.util.toIntUp
import kotlin.math.roundToInt
import kotlin.random.Random
import com.google.android.material.R as materialR

class BarChartView @JvmOverloads constructor(
	context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

	private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
	private val rawData = ArrayList<Bar>()
	private val bars = ArrayList<Bar>()
	private var maxValue: Int = 0
	private val minBarSpacing = context.resources.resolveDp(12f)
	private val minSpace = context.resources.resolveDp(20f)
	private val barWidth = context.resources.resolveDp(12f)
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
	var barColor: Int = context.getThemeColor(materialR.attr.colorAccent)
		set(value) {
			field = value
			invalidate()
		}

	init {
		paint.strokeWidth = context.resources.resolveDp(1f)
		if (isInEditMode) {
			setData(
				List(Random.nextInt(20, 60)) {
					Bar(
						value = Random.nextInt(-20, 400).coerceAtLeast(0),
						label = it.toString(),
					)
				},
			)
		}
	}

	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)
		if (bars.isEmpty() || chartBounds.isEmpty) {
			return
		}
		val spacing = (chartBounds.width() - (barWidth * bars.size.toFloat())) / (bars.size + 1).toFloat()
		// dashed horizontal lines
		paint.color = outlineColor
		paint.style = Paint.Style.STROKE
		canvas.drawLine(chartBounds.left, chartBounds.bottom, chartBounds.right, chartBounds.bottom, paint)
		paint.pathEffect = dottedEffect
		for (i in (0..maxValue).step(computeValueStep())) {
			val y = chartBounds.top + (chartBounds.height() * i / maxValue.toFloat())
			canvas.drawLine(paddingLeft.toFloat(), y, (width - paddingLeft - paddingRight).toFloat(), y, paint)
		}
		// bottom line
		paint.color = outlineColor
		paint.style = Paint.Style.STROKE
		canvas.drawLine(chartBounds.left, chartBounds.bottom, chartBounds.right, chartBounds.bottom, paint)
		// bars
		paint.style = Paint.Style.FILL
		paint.color = barColor
		paint.pathEffect = null
		val corner = barWidth / 2f
		for ((i, bar) in bars.withIndex()) {
			if (bar.value == 0) {
				continue
			}
			val h = (chartBounds.height() * bar.value / maxValue.toFloat()).coerceAtLeast(barWidth)
			val x = spacing + i * (barWidth + spacing) + paddingLeft
			canvas.drawRoundRect(x, chartBounds.bottom - h, x + barWidth, chartBounds.bottom, corner, corner, paint)
		}
	}

	override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
		super.onLayout(changed, left, top, right, bottom)
		invalidateBounds()
	}

	fun setData(value: List<Bar>) {
		rawData.replaceWith(value)
		compressBars()
		invalidate()
	}

	private fun compressBars() {
		if (rawData.isEmpty() || width <= 0) {
			maxValue = 0
			bars.clear()
			return
		}
		val fullWidth = rawData.size * (barWidth + minBarSpacing) + minBarSpacing
		val windowSize = (fullWidth / width.toFloat()).toIntUp()
		bars.replaceWith(
			rawData.chunked(windowSize) { it.average() },
		)
		maxValue = bars.maxOf { it.value }
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
		compressBars()
	}

	private fun Collection<Bar>.average(): Bar {
		return when (size) {
			0 -> Bar(0, "")
			1 -> first()
			else -> Bar(
				value = (sumOf { it.value } / size.toFloat()).roundToInt(),
				label = "%s - %s".format(first().label, last().label),
			)
		}
	}

	class Bar(
		val value: Int,
		val label: String,
	)
}
