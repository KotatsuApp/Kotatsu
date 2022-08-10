package org.koitharu.kotatsu.base.ui.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.core.graphics.ColorUtils
import com.google.android.material.R as materialR
import kotlin.random.Random
import org.koitharu.kotatsu.parsers.util.replaceWith
import org.koitharu.kotatsu.utils.ext.getThemeColor
import org.koitharu.kotatsu.utils.ext.resolveDp

class SegmentedBarView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

	private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
	private val segmentsData = ArrayList<Segment>()
	private val segmentsSizes = ArrayList<Float>()
	private val outlineColor = context.getThemeColor(materialR.attr.colorOutline)
	private var cornerSize = 0f

	var segments: List<Segment>
		get() = segmentsData
		set(value) {
			segmentsData.replaceWith(value)
			updateSizes()
			invalidate()
		}

	init {
		paint.strokeWidth = context.resources.resolveDp(1f)
		outlineProvider = OutlineProvider()
		clipToOutline = true

		if (isInEditMode) {
			segments = List(Random.nextInt(3, 5)) {
				Segment(
					percent = Random.nextFloat(),
					color = ColorUtils.HSLToColor(floatArrayOf(Random.nextInt(0, 360).toFloat(), 0.5f, 0.5f)),
				)
			}
		}
	}

	override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
		super.onSizeChanged(w, h, oldw, oldh)
		cornerSize = h / 2f
		updateSizes()
	}

	override fun onDraw(canvas: Canvas) {
		if (segmentsSizes.isEmpty()) {
			return
		}
		val w = width.toFloat()
		var x = w - segmentsSizes.last()
		for (i in (0 until segmentsData.size).reversed()) {
			val segment = segmentsData[i]
			paint.color = segment.color
			paint.style = Paint.Style.FILL
			val segmentWidth = segmentsSizes[i]
			canvas.drawRoundRect(0f, 0f, x + cornerSize, height.toFloat(), cornerSize, cornerSize, paint)
			paint.color = outlineColor
			paint.style = Paint.Style.STROKE
			canvas.drawRoundRect(0f, 0f, x + cornerSize, height.toFloat(), cornerSize, cornerSize, paint)
			x -= segmentWidth
		}
		paint.color = outlineColor
		paint.style = Paint.Style.STROKE
		canvas.drawRoundRect(0f, 0f, w, height.toFloat(), cornerSize, cornerSize, paint)
	}

	private fun updateSizes() {
		segmentsSizes.clear()
		segmentsSizes.ensureCapacity(segmentsData.size + 1)
		var w = width.toFloat()
		for (segment in segmentsData) {
			val segmentWidth = (w * segment.percent).coerceAtLeast(cornerSize)
			segmentsSizes.add(segmentWidth)
			w -= segmentWidth
		}
		segmentsSizes.add(w)
	}

	class Segment(
		@FloatRange(from = 0.0, to = 1.0) val percent: Float,
		@ColorInt val color: Int,
	) {

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as Segment

			if (percent != other.percent) return false
			if (color != other.color) return false

			return true
		}

		override fun hashCode(): Int {
			var result = percent.hashCode()
			result = 31 * result + color
			return result
		}
	}

	private class OutlineProvider : ViewOutlineProvider() {
		override fun getOutline(view: View, outline: Outline) {
			outline.setRoundRect(0, 0, view.width, view.height, view.height / 2f)
		}
	}
}
