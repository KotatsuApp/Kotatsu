package org.koitharu.kotatsu.reader.ui.pager.webtoon

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.core.view.ViewCompat.TYPE_TOUCH
import androidx.core.view.forEach
import androidx.recyclerview.widget.RecyclerView
import org.koitharu.kotatsu.core.util.ext.findCenterViewPosition
import java.util.LinkedList
import java.util.WeakHashMap

class WebtoonRecyclerView @JvmOverloads constructor(
	context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

	private var onPageScrollListeners: MutableList<OnPageScrollListener>? = null
	private val detachedViews = WeakHashMap<View, Unit>()

	override fun onChildDetachedFromWindow(child: View) {
		super.onChildDetachedFromWindow(child)
		detachedViews[child] = Unit
	}

	override fun onChildAttachedToWindow(child: View) {
		super.onChildAttachedToWindow(child)
		detachedViews.remove(child)
	}

	override fun startNestedScroll(axes: Int) = startNestedScroll(axes, TYPE_TOUCH)

	override fun startNestedScroll(axes: Int, type: Int): Boolean = true

	override fun dispatchNestedPreScroll(
		dx: Int,
		dy: Int,
		consumed: IntArray?,
		offsetInWindow: IntArray?
	) = dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, TYPE_TOUCH)

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
		notifyScrollChanged(dy)
		return consumedY != 0 || dy == 0
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
						val unconsumed =
							dy - consumedByChild - nextChild.top //will be consumed by scroll
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
						val unconsumed =
							dy - consumedByChild + (height - nextChild.bottom) //will be consumed by scroll
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

	fun addOnPageScrollListener(listener: OnPageScrollListener) {
		val list = onPageScrollListeners ?: LinkedList<OnPageScrollListener>().also { onPageScrollListeners = it }
		list.add(listener)
	}

	fun removeOnPageScrollListener(listener: OnPageScrollListener) {
		onPageScrollListeners?.remove(listener)
	}

	private fun notifyScrollChanged(dy: Int) {
		val listeners = onPageScrollListeners
		if (listeners.isNullOrEmpty()) {
			return
		}
		val centerPosition = findCenterViewPosition()
		listeners.forEach { it.dispatchScroll(this, dy, centerPosition) }
	}

	fun relayoutChildren() {
		forEach { child ->
			(child as WebtoonFrameLayout).target.requestLayout()
		}
		detachedViews.keys.forEach { child ->
			(child as WebtoonFrameLayout).target.requestLayout()
		}
	}

	abstract class OnPageScrollListener {

		private var lastPosition = NO_POSITION

		fun dispatchScroll(recyclerView: WebtoonRecyclerView, dy: Int, centerPosition: Int) {
			onScroll(recyclerView, dy)
			if (centerPosition != NO_POSITION && centerPosition != lastPosition) {
				lastPosition = centerPosition
				onPageChanged(recyclerView, centerPosition)
			}
		}

		open fun onScroll(recyclerView: WebtoonRecyclerView, dy: Int) = Unit

		open fun onPageChanged(recyclerView: WebtoonRecyclerView, index: Int) = Unit
	}
}
