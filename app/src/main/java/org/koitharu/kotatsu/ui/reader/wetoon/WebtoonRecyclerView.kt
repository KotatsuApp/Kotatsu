package org.koitharu.kotatsu.ui.reader.wetoon

import android.content.Context
import android.util.AttributeSet
import androidx.core.view.ViewCompat
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView

class WebtoonRecyclerView @JvmOverloads constructor(
	context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

	override fun startNestedScroll(axes: Int) = startNestedScroll(axes, ViewCompat.TYPE_TOUCH)

	override fun startNestedScroll(axes: Int, type: Int): Boolean {
		return true
	}

	override fun dispatchNestedPreScroll(
		dx: Int,
		dy: Int,
		consumed: IntArray?,
		offsetInWindow: IntArray?
	) = dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, ViewCompat.TYPE_TOUCH)

	override fun dispatchNestedPreScroll(
		dx: Int,
		dy: Int,
		consumed: IntArray?,
		offsetInWindow: IntArray?,
		type: Int
	): Boolean {
		val consumedY = consumeVerticalScroll(dy)
		if (consumed != null) {
			consumed[0] = 0
			consumed[1] = consumedY
		}
		return consumedY != 0
	}

	private fun consumeVerticalScroll(dy: Int): Int {
		val child = when {
			dy > 0 -> children.firstOrNull { it is WebtoonFrameLayout }
			dy < 0 -> children.lastOrNull { it is WebtoonFrameLayout }
			else -> null
		} ?: return 0
		var scrollY = dy
		scrollY -= (child as WebtoonFrameLayout).dispatchVerticalScroll(scrollY)
		return dy - scrollY
	}
}