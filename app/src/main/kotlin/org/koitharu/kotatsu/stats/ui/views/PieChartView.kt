package org.koitharu.kotatsu.stats.ui.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.ColorUtils
import org.koitharu.kotatsu.core.util.ext.getThemeColor
import org.koitharu.kotatsu.core.util.ext.resolveDp
import org.koitharu.kotatsu.parsers.util.replaceWith
import kotlin.math.sqrt

class PieChartView @JvmOverloads constructor(
	context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), GestureDetector.OnGestureListener {

	private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
	private val segments = ArrayList<Segment>()
	private val chartBounds = RectF()
	private val clearColor = context.getThemeColor(android.R.attr.colorBackground)
	private val touchDetector = GestureDetector(context, this)
	private var highlightedSegment = -1

	var onSegmentClickListener: OnSegmentClickListener? = null

	init {
		touchDetector.setIsLongpressEnabled(false)
		paint.strokeWidth = context.resources.resolveDp(2f)
	}

	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)
		var angle = 0f
		for ((i, segment) in segments.withIndex()) {
			paint.color = segment.color
			if (i == highlightedSegment) {
				paint.color = ColorUtils.setAlphaComponent(paint.color, 180)
			}
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

	@SuppressLint("ClickableViewAccessibility")
	override fun onTouchEvent(event: MotionEvent): Boolean {
		if (event.actionMasked == MotionEvent.ACTION_CANCEL || event.actionMasked == MotionEvent.ACTION_UP) {
			highlightedSegment = -1
			invalidate()
		}
		return super.onTouchEvent(event) || touchDetector.onTouchEvent(event)
	}

	override fun onDown(e: MotionEvent): Boolean {
		if (onSegmentClickListener == null) {
			return false
		}
		val segment = findSegmentIndex(e.x, e.y)
		if (segment != highlightedSegment) {
			highlightedSegment = segment
			invalidate()
			return true
		} else {
			return false
		}
	}

	override fun onShowPress(e: MotionEvent) = Unit

	override fun onSingleTapUp(e: MotionEvent): Boolean {
		onSegmentClickListener?.run {
			val segment = segments.getOrNull(findSegmentIndex(e.x, e.y))
			if (segment != null) {
				onSegmentClick(this@PieChartView, segment)
			}
		}
		return true
	}

	override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean = false

	override fun onLongPress(e: MotionEvent) = Unit

	override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean = false

	fun setData(value: List<Segment>) {
		segments.replaceWith(value)
		invalidate()
	}

	private fun findSegmentIndex(x: Float, y: Float): Int {
		val dy = (y - chartBounds.centerY()).toDouble()
		val dx = (x - chartBounds.centerX()).toDouble()
		val distance = sqrt(dx * dx + dy * dy).toFloat()
		if (distance < chartBounds.height() / 4f || distance > chartBounds.centerX()) {
			return -1
		}
		var touchAngle = Math.toDegrees(Math.atan2(dy, dx)).toFloat()
		if (touchAngle < 0) {
			touchAngle += 360
		}
		var angle = 0f
		for ((i, segment) in segments.withIndex()) {
			val sweepAngle = segment.percent * 360f
			if (touchAngle in angle..(angle + sweepAngle)) {
				return i
			}
			angle += sweepAngle
		}
		return -1
	}

	class Segment(
		val value: Int,
		val label: String,
		val percent: Float,
		val color: Int,
		val tag: Any?,
	)

	interface OnSegmentClickListener {

		fun onSegmentClick(view: PieChartView, segment: Segment)
	}
}
