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
import org.koitharu.kotatsu.parsers.util.replaceWith
import org.koitharu.kotatsu.utils.ext.resolveDp
import kotlin.random.Random

class SegmentedBarView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

	private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
	private val segmentsData = ArrayList<Segment>()
	private val minSegmentSize = context.resources.resolveDp(3f)

	var segments: List<Segment>
		get() = segmentsData
		set(value) {
			segmentsData.replaceWith(value)
			invalidate()
		}

	init {
		paint.style = Paint.Style.FILL
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

	override fun onDraw(canvas: Canvas) {
		var x = 0f
		val w = width.toFloat()
		for (segment in segmentsData) {
			paint.color = segment.color
			val segmentWidth = (w * segment.percent).coerceAtLeast(minSegmentSize)
			canvas.drawRect(x, 0f, x + segmentWidth, height.toFloat(), paint)
			x += segmentWidth
		}
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