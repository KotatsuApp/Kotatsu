package org.koitharu.kotatsu.reader.ui.tapgrid

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import org.koitharu.kotatsu.reader.domain.TapGridArea
import kotlin.math.roundToInt

class TapGridDispatcher(
	context: Context,
	private val listener: OnGridTouchListener,
) : GestureDetector.SimpleOnGestureListener() {

	private val detector = GestureDetector(context, this)
	private val width = context.resources.displayMetrics.widthPixels
	private val height = context.resources.displayMetrics.heightPixels
	private var isDispatching = false

	init {
		detector.setIsLongpressEnabled(true)
		detector.setOnDoubleTapListener(this)
	}

	fun dispatchTouchEvent(event: MotionEvent) {
		if (event.actionMasked == MotionEvent.ACTION_DOWN) {
			isDispatching = listener.onProcessTouch(event.rawX.toInt(), event.rawY.toInt())
		}
		detector.onTouchEvent(event)
	}

	override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
		if (!isDispatching) {
			return true
		}
		val area = getArea(event.rawX, event.rawY) ?: return false
		return listener.onGridTouch(area)
	}

	override fun onDoubleTapEvent(e: MotionEvent): Boolean {
		isDispatching = false // ignore long press after double tap
		return super.onDoubleTapEvent(e)
	}

	override fun onLongPress(event: MotionEvent) {
		if (isDispatching) {
			val area = getArea(event.rawX, event.rawY) ?: return
			listener.onGridLongTouch(area)
		}
	}

	private fun getArea(x: Float, y: Float): TapGridArea? {
		val xIndex = (x * 2f / width).roundToInt()
		val yIndex = (y * 2f / height).roundToInt()
		val area = when (xIndex) {
			0 -> when (yIndex) { // LEFT
				0 -> TapGridArea.TOP_LEFT
				1 -> TapGridArea.CENTER_LEFT
				2 -> TapGridArea.BOTTOM_LEFT
				else -> null
			}

			1 -> when (yIndex) { // CENTER
				0 -> TapGridArea.TOP_CENTER
				1 -> TapGridArea.CENTER
				2 -> TapGridArea.BOTTOM_CENTER
				else -> null
			}

			2 -> when (yIndex) { // RIGHT
				0 -> TapGridArea.TOP_RIGHT
				1 -> TapGridArea.CENTER_RIGHT
				2 -> TapGridArea.BOTTOM_RIGHT
				else -> null
			}

			else -> null
		}
		assert(area != null) { "Invalid area ($xIndex, $yIndex)" }
		return area
	}

	interface OnGridTouchListener {

		fun onGridTouch(area: TapGridArea): Boolean

		fun onGridLongTouch(area: TapGridArea)

		fun onProcessTouch(rawX: Int, rawY: Int): Boolean
	}
}
