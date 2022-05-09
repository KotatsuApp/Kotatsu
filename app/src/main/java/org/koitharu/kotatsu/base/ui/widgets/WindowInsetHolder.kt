package org.koitharu.kotatsu.base.ui.widgets

import android.annotation.SuppressLint
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

	@SuppressLint("RtlHardcoded")
	override fun dispatchApplyWindowInsets(insets: WindowInsets): WindowInsets {
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
		return super.dispatchApplyWindowInsets(insets)
	}

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		val widthMode = MeasureSpec.getMode(widthMeasureSpec)
		val heightMode = MeasureSpec.getMode(heightMeasureSpec)
		super.onMeasure(
			if (desiredWidth == 0 || widthMode == MeasureSpec.EXACTLY) {
				widthMeasureSpec
			} else {
				MeasureSpec.makeMeasureSpec(desiredWidth, widthMode)
			},
			if (desiredHeight == 0 || heightMode == MeasureSpec.EXACTLY) {
				heightMeasureSpec
			} else {
				MeasureSpec.makeMeasureSpec(desiredHeight, heightMode)
			},
		)
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