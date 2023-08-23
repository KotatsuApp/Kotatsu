package org.koitharu.kotatsu.reader.ui.pager.webtoon

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.OverScroller
import androidx.core.view.GestureDetectorCompat

private const val MAX_SCALE = 2.5f
private const val MIN_SCALE = 0.5f

class WebtoonScalingFrame @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyles: Int = 0,
) : FrameLayout(context, attrs, defStyles), ScaleGestureDetector.OnScaleGestureListener {

	private val targetChild by lazy(LazyThreadSafetyMode.NONE) { getChildAt(0) as WebtoonRecyclerView }

	private val scaleDetector = ScaleGestureDetector(context, this)
	private val gestureDetector = GestureDetectorCompat(context, GestureListener())
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

	var isZoomEnable = true
		set(value) {
			field = value
			if (scale != 1f) {
				scaleChild(1f, halfWidth, halfHeight)
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

		gestureDetector.onTouchEvent(ev)
		scaleDetector.onTouchEvent(ev)

		// Offset event to inside the child view
		if (scale < 1 && !targetHitRect.contains(ev.x.toInt(), ev.y.toInt())) {
			ev.offsetLocation(halfWidth - ev.x + targetHitRect.width() / 3, 0f)
		}

		// Send action cancel to avoid recycler jump when scale end
		if (scaleDetector.isInProgress) {
			ev.action = MotionEvent.ACTION_CANCEL
		}
		return super.dispatchTouchEvent(ev)
	}

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec)
		halfWidth = measuredWidth / 2f
		halfHeight = measuredHeight / 2f
	}

	private fun invalidateTarget() {
		adjustBounds()
		targetChild.run {
			scaleX = scale
			scaleY = scale
			translationX = transX
			translationY = transY
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

		transformMatrix.postTranslate(dx, dy)
		syncMatrixValues()
	}

	private fun scaleChild(newScale: Float, focusX: Float, focusY: Float) {
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
	}


	override fun onScale(detector: ScaleGestureDetector): Boolean {
		val newScale = (scale * detector.scaleFactor).coerceIn(MIN_SCALE, MAX_SCALE)
		scaleChild(newScale, detector.focusX, detector.focusY)
		return true
	}

	override fun onScaleBegin(detector: ScaleGestureDetector): Boolean = true

	override fun onScaleEnd(p0: ScaleGestureDetector) = Unit


	private inner class GestureListener : GestureDetector.SimpleOnGestureListener(), Runnable {

		override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
			if (scale <= 1f) return false
			transformMatrix.postTranslate(-distanceX, -distanceY)
			invalidateTarget()
			return true
		}

		override fun onDoubleTap(e: MotionEvent): Boolean {
			val newScale = if (scale != 1f) 1f else MAX_SCALE * 0.8f
			ValueAnimator.ofFloat(scale, newScale).run {
				interpolator = AccelerateDecelerateInterpolator()
				duration = 300
				addUpdateListener {
					scaleChild(it.animatedValue as Float, e.x, e.y)
				}
				start()
			}
			return true
		}

		override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
			if (scale <= 1) return false

			overScroller.fling(
				transX.toInt(),
				transY.toInt(),
				velocityX.toInt(),
				velocityY.toInt(),
				translateBounds.left.toInt(),
				translateBounds.right.toInt(),
				translateBounds.top.toInt(),
				translateBounds.bottom.toInt(),
			)
			postOnAnimation(this)
			return true
		}

		override fun run() {
			if (overScroller.computeScrollOffset()) {
				transformMatrix.postTranslate(overScroller.currX - transX, overScroller.currY - transY)
				invalidateTarget()
				postOnAnimation(this)
			}
		}
	}
}
