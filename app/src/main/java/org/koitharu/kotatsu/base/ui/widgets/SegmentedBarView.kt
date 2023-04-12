package org.koitharu.kotatsu.base.ui.widgets

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import android.view.animation.DecelerateInterpolator
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import org.koitharu.kotatsu.parsers.util.replaceWith
import org.koitharu.kotatsu.utils.ext.getAnimationDuration
import org.koitharu.kotatsu.utils.ext.getThemeColor
import org.koitharu.kotatsu.utils.ext.isAnimationsEnabled
import org.koitharu.kotatsu.utils.ext.resolveDp
import com.google.android.material.R as materialR

class SegmentedBarView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr), ValueAnimator.AnimatorUpdateListener, Animator.AnimatorListener {

	private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
	private val segmentsData = ArrayList<Segment>()
	private val segmentsSizes = ArrayList<Float>()
	private val outlineColor = context.getThemeColor(materialR.attr.colorOutline)
	private var cornerSize = 0f
	private var scaleFactor = 1f
	private var scaleAnimator: ValueAnimator? = null

	init {
		paint.strokeWidth = context.resources.resolveDp(1f)
		outlineProvider = OutlineProvider()
		clipToOutline = true
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

	override fun onAnimationStart(animation: Animator) = Unit

	override fun onAnimationEnd(animation: Animator) {
		if (scaleAnimator === animation) {
			scaleAnimator = null
		}
	}

	override fun onAnimationUpdate(animation: ValueAnimator) {
		scaleFactor = animation.animatedValue as Float
		updateSizes()
		invalidate()
	}

	override fun onAnimationCancel(animation: Animator) = Unit

	override fun onAnimationRepeat(animation: Animator) = Unit

	fun animateSegments(value: List<Segment>) {
		scaleAnimator?.cancel()
		segmentsData.replaceWith(value)
		if (!context.isAnimationsEnabled) {
			scaleAnimator = null
			scaleFactor = 1f
			updateSizes()
			invalidate()
			return
		}
		scaleFactor = 0f
		updateSizes()
		invalidate()
		val animator = ValueAnimator.ofFloat(0f, 1f)
		animator.duration = context.getAnimationDuration(android.R.integer.config_longAnimTime)
		animator.interpolator = DecelerateInterpolator()
		animator.addUpdateListener(this@SegmentedBarView)
		animator.addListener(this@SegmentedBarView)
		scaleAnimator = animator
		animator.start()
	}

	private fun updateSizes() {
		segmentsSizes.clear()
		segmentsSizes.ensureCapacity(segmentsData.size + 1)
		var w = width.toFloat()
		val maxScale = (scaleFactor * (segmentsData.size - 1)).coerceAtLeast(1f)
		for ((index, segment) in segmentsData.withIndex()) {
			val scale = (scaleFactor * (index + 1) / maxScale).coerceAtMost(1f)
			val segmentWidth = (w * segment.percent).coerceAtLeast(
				if (index == 0) height.toFloat() else cornerSize,
			) * scale
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
