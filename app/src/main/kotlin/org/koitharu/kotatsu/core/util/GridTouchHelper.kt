package org.koitharu.kotatsu.core.util

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import kotlin.math.roundToInt

class GridTouchHelper(
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
		val xIndex = (event.rawX * 2f / width).roundToInt()
		val yIndex = (event.rawY * 2f / height).roundToInt()
		listener.onGridTouch(
			when (xIndex) {
				0 -> AREA_LEFT
				1 -> {
					when (yIndex) {
						0 -> AREA_TOP
						1 -> AREA_CENTER
						2 -> AREA_BOTTOM
						else -> return false
					}
				}

				2 -> AREA_RIGHT
				else -> return false
			},
		)
		return true
	}

	companion object {

		const val AREA_CENTER = 1
		const val AREA_LEFT = 2
		const val AREA_RIGHT = 3
		const val AREA_TOP = 4
		const val AREA_BOTTOM = 5
	}

	interface OnGridTouchListener {

		fun onGridTouch(area: Int)

		fun onProcessTouch(rawX: Int, rawY: Int): Boolean
	}
}
