package org.koitharu.kotatsu.core.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.AttrRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.WindowInsetsCompat

class WindowInsetHolder @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	@AttrRes defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

	private var desiredHeight = 0
	private var desiredWidth = 0

	override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
		val barsInsets = WindowInsetsCompat.toWindowInsetsCompat(insets, this)
			.getInsets(WindowInsetsCompat.Type.systemBars())
		val gravity = getLayoutGravity()
		val newWidth = when (gravity and Gravity.HORIZONTAL_GRAVITY_MASK) {
			Gravity.LEFT -> barsInsets.left
			Gravity.RIGHT -> barsInsets.right
			else -> 0
		}
		val newHeight = when (gravity and Gravity.VERTICAL_GRAVITY_MASK) {
			Gravity.TOP -> barsInsets.top
			Gravity.BOTTOM -> barsInsets.bottom
			else -> 0
		}
		if (newWidth != desiredWidth || newHeight != desiredHeight) {
			desiredWidth = newWidth
			desiredHeight = newHeight
			requestLayout()
		}
		return super.onApplyWindowInsets(insets)
	}

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		val widthMode = MeasureSpec.getMode(widthMeasureSpec)
		val widthSize = MeasureSpec.getSize(widthMeasureSpec)
		val heightMode = MeasureSpec.getMode(heightMeasureSpec)
		val heightSize = MeasureSpec.getSize(heightMeasureSpec)

		val width: Int = when (widthMode) {
			MeasureSpec.EXACTLY -> widthSize
			MeasureSpec.AT_MOST -> minOf(desiredWidth, widthSize)
			else -> desiredWidth
		}
		val height = when (heightMode) {
			MeasureSpec.EXACTLY -> heightSize
			MeasureSpec.AT_MOST -> minOf(desiredHeight, heightSize)
			else -> desiredHeight
		}
		setMeasuredDimension(width, height)
	}

	private fun getLayoutGravity(): Int {
		return when (val lp = layoutParams) {
			is FrameLayout.LayoutParams -> lp.gravity
			is LinearLayout.LayoutParams -> lp.gravity
			is CoordinatorLayout.LayoutParams -> lp.gravity
			else -> Gravity.NO_GRAVITY
		}
	}
}
