package org.koitharu.kotatsu.reader.ui.pager.webtoon

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Matrix
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ViewConfiguration
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.OverScroller
import androidx.core.animation.doOnEnd
import androidx.core.view.ViewConfigurationCompat
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.widgets.ZoomControl
import org.koitharu.kotatsu.core.util.ext.getAnimationDuration
import kotlin.math.roundToInt

private const val MAX_SCALE = 2.5f
private const val MIN_SCALE = 0.5f

private const val FLING_RANGE = 20_000

class WebtoonScalingFrame @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyles: Int = 0,
) : FrameLayout(context, attrs, defStyles),
	ScaleGestureDetector.OnScaleGestureListener,
	ZoomControl.ZoomControlListener {

	private val scaleDetector = ScaleGestureDetector(context, this)
	private val gestureDetector = GestureDetector(context, GestureListener())
	private val overScroller = OverScroller(context, AccelerateDecelerateInterpolator())

	private val transformMatrix = Matrix()
	private val matrixValues = FloatArray(9)
	private val scale
		get() = matrixValues[Matrix.MSCALE_X]
	private val transX
		get() = halfWidth * (scale - 1f) + matrixValues[Matrix.MTRANS_X]
	private val transY
		get() = halfHeight * (scale - 1f) + matrixValues[Matrix.MTRANS_Y]
	private var halfWidth = 0f
	private var halfHeight = 0f
	private val translateBounds = RectF()
	private val targetHitRect = Rect()
	private var animator: ValueAnimator? = null
	private var pendingScroll = 0

	var isZoomEnable = false
		set(value) {
			field = value
			if (scale != 1f) {
				scaleChild(1f, halfWidth, halfHeight)
			}
		}

	var zoom: Float
		get() = scale
		set(value) {
			if (value != scale) {
				scaleChild(value, halfWidth, halfHeight)
				onPostScale(invalidateLayout = true)
			}
		}

	init {
		syncMatrixValues()
	}

	override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
		if (!isZoomEnable || ev == null) {
			return super.dispatchTouchEvent(ev)
		}

		if (ev.action == MotionEvent.ACTION_DOWN && overScroller.computeScrollOffset()) {
			overScroller.forceFinished(true)
		}

		val consumed = gestureDetector.onTouchEvent(ev)
		scaleDetector.onTouchEvent(ev)

		// Offset event to inside the child view
		if (scale < 1 && !targetHitRect.contains(ev.x.toInt(), ev.y.toInt())) {
			ev.offsetLocation(halfWidth - ev.x + targetHitRect.width() / 3, 0f)
		}

		return consumed || scaleDetector.isInProgress || super.dispatchTouchEvent(ev)
	}

	override fun onGenericMotionEvent(event: MotionEvent): Boolean {
		if (isZoomEnable && event.source and InputDevice.SOURCE_CLASS_POINTER != 0) {
			if (event.actionMasked == MotionEvent.ACTION_SCROLL) {
				val withCtrl = event.metaState and KeyEvent.META_CTRL_MASK != 0
				if (withCtrl) {
					val axisValue =
						event.getAxisValue(MotionEvent.AXIS_VSCROLL) * ViewConfigurationCompat.getScaledVerticalScrollFactor(
							ViewConfiguration.get(context), context,
						)
					val newScale = (scale + axisValue).coerceIn(MIN_SCALE, MAX_SCALE)
					scaleChild(newScale, event.x, event.y)
					return true
				}
			}
		}
		return super.onGenericMotionEvent(event)
	}

	override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
		if (!isZoomEnable) {
			return super.onKeyDown(keyCode, event)
		}
		return when (keyCode) {
			KeyEvent.KEYCODE_ZOOM_IN,
			KeyEvent.KEYCODE_NUMPAD_ADD,
			KeyEvent.KEYCODE_PLUS -> {
				onZoomIn()
				true
			}

			KeyEvent.KEYCODE_ZOOM_OUT,
			KeyEvent.KEYCODE_NUMPAD_SUBTRACT,
			KeyEvent.KEYCODE_MINUS -> {
				onZoomOut()
				true
			}

			KeyEvent.KEYCODE_ESCAPE -> {
				smoothScaleTo(1f)
				true
			}

			else -> super.onKeyDown(keyCode, event)
		}
	}

	override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
		return if (isZoomEnable) {
			keyCode == KeyEvent.KEYCODE_NUMPAD_ADD
				|| keyCode == KeyEvent.KEYCODE_PLUS
				|| keyCode == KeyEvent.KEYCODE_NUMPAD_SUBTRACT
				|| keyCode == KeyEvent.KEYCODE_MINUS
				|| keyCode == KeyEvent.KEYCODE_ZOOM_IN
				|| keyCode == KeyEvent.KEYCODE_ZOOM_OUT
				|| keyCode == KeyEvent.KEYCODE_ESCAPE
				|| super.onKeyUp(keyCode, event)
		} else {
			super.onKeyUp(keyCode, event)
		}
	}

	override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
		super.onSizeChanged(w, h, oldw, oldh)
		halfWidth = w / 2f
		halfHeight = h / 2f
	}

	override fun onZoomIn() {
		smoothScaleTo(scale * 1.1f)
	}

	override fun onZoomOut() {
		smoothScaleTo(scale * 0.9f)
	}

	private fun invalidateTarget() {
		val targetChild = findTargetChild()
		adjustBounds()
		targetChild.run {
			if (!scale.isNaN()) {
				scaleX = scale
				scaleY = scale
			}
			translationX = transX
			translationY = transY
			if (pendingScroll != 0) {
				nestedScrollBy(0, pendingScroll)
				pendingScroll = 0
			}
		}

		val newHeight = if (scale < 1f) (height / scale).toInt() else height
		if (newHeight != targetChild.height) {
			targetChild.layoutParams.height = newHeight
			targetChild.requestLayout()
			targetChild.relayoutChildren()
		}

		if (scale < 1) {
			targetChild.getHitRect(targetHitRect)
		}
	}

	private fun syncMatrixValues() {
		transformMatrix.getValues(matrixValues)
	}

	private fun adjustBounds() {
		syncMatrixValues()
		val dx = when {
			transX < translateBounds.left -> translateBounds.left - transX
			transX > translateBounds.right -> translateBounds.right - transX
			else -> 0f
		}

		val dy = when {
			transY < translateBounds.top -> translateBounds.top - transY
			transY > translateBounds.bottom -> translateBounds.bottom - transY
			else -> 0f
		}

		pendingScroll = if (scale > 1) (dy / scale).roundToInt() else 0
		transformMatrix.postTranslate(dx, dy)
		syncMatrixValues()
	}

	private fun scaleChild(
		newScale: Float,
		focusX: Float,
		focusY: Float,
	): Boolean {
		if (scale.isNaN() || scale == 0f) {
			return false
		}
		val factor = newScale / scale
		if (newScale > 1) {
			translateBounds.set(
				halfWidth * (1 - newScale),
				halfHeight * (1 - newScale),
				halfWidth * (newScale - 1),
				halfHeight * (newScale - 1),
			)
		} else {
			translateBounds.set(
				0f,
				halfHeight - halfHeight / newScale,
				0f,
				halfHeight - halfHeight / newScale,
			)
		}
		transformMatrix.postScale(factor, factor, focusX, focusY)
		invalidateTarget()
		return true
	}

	override fun onScale(detector: ScaleGestureDetector): Boolean {
		val newScale = (scale * detector.scaleFactor).coerceIn(MIN_SCALE, MAX_SCALE)
		return scaleChild(newScale, detector.focusX, detector.focusY)
	}

	override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
		animator?.cancel()
		animator = null
		return true
	}

	override fun onScaleEnd(p0: ScaleGestureDetector) {
		onPostScale(invalidateLayout = false)
	}

	private fun onPostScale(invalidateLayout: Boolean) {
		val target = findTargetChild()
		target.post {
			target.updateChildrenScroll()
			if (invalidateLayout) {
				target.requestLayout()
			}
		}
	}

	private fun smoothScaleTo(target: Float) {
		val newScale = target.coerceIn(MIN_SCALE, MAX_SCALE)
		animator?.cancel()
		animator = ValueAnimator.ofFloat(scale, newScale).apply {
			setDuration(context.getAnimationDuration(android.R.integer.config_shortAnimTime))
			interpolator = DecelerateInterpolator()
			addUpdateListener { scaleChild(it.animatedValue as Float, halfWidth, halfHeight) }
			doOnEnd { onPostScale(invalidateLayout = false) }
			start()
		}
	}

	private fun findTargetChild() = getChildAt(0) as WebtoonRecyclerView

	private inner class GestureListener : GestureDetector.SimpleOnGestureListener(), Runnable {
		private val prevPos = Point()

		override fun onScroll(
			e1: MotionEvent?,
			e2: MotionEvent,
			distanceX: Float,
			distanceY: Float,
		): Boolean {
			if (scale <= 1f || scale.isNaN()) return false
			transformMatrix.postTranslate(-distanceX, -distanceY)
			invalidateTarget()
			return true
		}

		override fun onDoubleTap(e: MotionEvent): Boolean {
			val newScale = if (scale != 1f) 1f else MAX_SCALE * 0.8f
			ValueAnimator.ofFloat(scale, newScale).run {
				interpolator = AccelerateDecelerateInterpolator()
				duration = context.getAnimationDuration(R.integer.config_defaultAnimTime)
				addUpdateListener {
					scaleChild(it.animatedValue as Float, e.x, e.y)
				}
				start()
			}
			return true
		}

		override fun onFling(
			e1: MotionEvent?,
			e2: MotionEvent,
			velocityX: Float,
			velocityY: Float,
		): Boolean {
			if (scale <= 1 || scale.isNaN()) return false

			prevPos.set(transX.toInt(), transY.toInt())
			overScroller.fling(
				prevPos.x,
				prevPos.y,
				velocityX.toInt(),
				velocityY.toInt(),
				translateBounds.left.toInt(),
				translateBounds.right.toInt(),
				translateBounds.top.toInt() - FLING_RANGE,
				translateBounds.bottom.toInt() + FLING_RANGE,
			)
			postOnAnimation(this)
			return true
		}

		override fun run() {
			if (overScroller.computeScrollOffset()) {
				transformMatrix.postTranslate(
					overScroller.currX.toFloat() - prevPos.x,
					overScroller.currY.toFloat() - prevPos.y,
				)
				prevPos.set(overScroller.currX, overScroller.currY)
				invalidateTarget()
				postOnAnimation(this)
			}
		}
	}
}
