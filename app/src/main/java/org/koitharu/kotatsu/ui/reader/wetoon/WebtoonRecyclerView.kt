package org.koitharu.kotatsu.ui.reader.wetoon

import android.content.Context
import android.util.AttributeSet
import androidx.core.view.ViewCompat
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
		if (childCount == 0) {
			return 0
		}
		when {
			dy > 0 -> {
				val child = getChildAt(0) as WebtoonFrameLayout
				var consumedByChild = child.dispatchVerticalScroll(dy)
				if (consumedByChild < dy) {
					if (childCount > 1) {
						val nextChild = getChildAt(1) as WebtoonFrameLayout
						val unconsumed = dy - consumedByChild - nextChild.top //will be consumed by scroll
						if (unconsumed > 0) {
							consumedByChild += nextChild.dispatchVerticalScroll(unconsumed)
						}
					}
				}
				return consumedByChild
			}
			dy < 0 -> {
				val child = getChildAt(childCount - 1) as WebtoonFrameLayout
				var consumedByChild = child.dispatchVerticalScroll(dy)
				if (consumedByChild > dy) {
					if (childCount > 1) {
						val nextChild = getChildAt(childCount - 2) as WebtoonFrameLayout
						val unconsumed = dy - consumedByChild + (height - nextChild.bottom) //will be consumed by scroll
						if (unconsumed < 0) {
							consumedByChild += nextChild.dispatchVerticalScroll(unconsumed)
						}
					}
				}
				return consumedByChild
			}
		}
		return 0
	}
}