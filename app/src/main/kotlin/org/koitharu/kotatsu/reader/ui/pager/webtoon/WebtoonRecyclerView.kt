package org.koitharu.kotatsu.reader.ui.pager.webtoon

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import android.widget.EdgeEffect
import androidx.core.view.ViewCompat.TYPE_TOUCH
import androidx.core.view.forEach
import androidx.core.view.isEmpty
import androidx.core.view.isNotEmpty
import androidx.core.view.iterator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.EdgeEffectFactory.DIRECTION_BOTTOM
import androidx.recyclerview.widget.RecyclerView.EdgeEffectFactory.DIRECTION_TOP
import java.util.Collections
import java.util.LinkedList
import java.util.WeakHashMap

class WebtoonRecyclerView @JvmOverloads constructor(
	context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

	private var onPageScrollListeners = LinkedList<OnWebtoonScrollListener>()
	private val scrollDispatcher = WebtoonScrollDispatcher()
	private val detachedViews = Collections.newSetFromMap(WeakHashMap<View, Boolean>())
	private var isFixingScroll = false

	var isPullGestureEnabled: Boolean = false
		set(value) {
			if (field != value) {
				field = value
				setEdgeEffectFactory(
					if (value) {
						PullEffect.Factory()
					} else {
						EdgeEffectFactory()
					},
				)
			}
		}
	var pullThreshold: Float = 0.3f
	private var pullListener: OnPullGestureListener? = null

	fun setOnPullGestureListener(listener: OnPullGestureListener?) {
		pullListener = listener
	}

	override fun onChildDetachedFromWindow(child: View) {
		super.onChildDetachedFromWindow(child)
		detachedViews.add(child)
	}

	override fun onChildAttachedToWindow(child: View) {
		super.onChildAttachedToWindow(child)
		detachedViews.remove(child)
	}

	override fun startNestedScroll(axes: Int) = startNestedScroll(axes, TYPE_TOUCH)

	override fun startNestedScroll(axes: Int, type: Int): Boolean = isNotEmpty()

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
		if (isEmpty()) {
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

	fun addOnPageScrollListener(listener: OnWebtoonScrollListener) {
		onPageScrollListeners.add(listener)
	}

	fun removeOnPageScrollListener(listener: OnWebtoonScrollListener) {
		onPageScrollListeners.remove(listener)
	}

	private fun notifyScrollChanged(dy: Int) {
		val listeners = onPageScrollListeners
		if (listeners.isEmpty()) {
			return
		}
		scrollDispatcher.dispatchScroll(this, dy)
	}

	fun relayoutChildren() {
		forEach { child ->
			(child as WebtoonFrameLayout).target.requestLayout()
		}
		detachedViews.forEach { child ->
			(child as WebtoonFrameLayout).target.requestLayout()
		}
	}

	fun updateChildrenScroll() {
		if (isFixingScroll) {
			return
		}
		isFixingScroll = true
		for (child in this) {
			val ssiv = (child as WebtoonFrameLayout).target
			if (adjustScroll(child, ssiv)) {
				break
			}
		}
		isFixingScroll = false
	}

	private fun adjustScroll(child: View, ssiv: WebtoonImageView): Boolean = when {
		child.bottom < height && ssiv.getScroll() < ssiv.getScrollRange() -> {
			val distance = minOf(height - child.bottom, ssiv.getScrollRange() - ssiv.getScroll())
			ssiv.scrollBy(distance)
			true
		}

		child.top > 0 && ssiv.getScroll() > 0 -> {
			val distance = minOf(child.top, ssiv.getScroll())
			ssiv.scrollBy(-distance)
			true
		}

		else -> false
	}

	private class WebtoonScrollDispatcher {

		private var firstPos = NO_POSITION
		private var lastPos = NO_POSITION

		fun dispatchScroll(rv: WebtoonRecyclerView, dy: Int) {
			val lm = rv.layoutManager as? LinearLayoutManager
			if (lm == null) {
				firstPos = NO_POSITION
				lastPos = NO_POSITION
				return
			}
			val newFirstPos = lm.findFirstVisibleItemPosition()
			val newLastPos = lm.findLastVisibleItemPosition()
			if (newFirstPos != firstPos || newLastPos != lastPos) {
				firstPos = newFirstPos
				lastPos = newLastPos
				if (newFirstPos != NO_POSITION && newLastPos != NO_POSITION) {
					rv.onPageScrollListeners.forEach { it.onScrollChanged(rv, dy, newFirstPos, newLastPos) }
				}
			}
		}
	}

	private class PullEffect(
		view: RecyclerView,
		private val direction: Int,
		private val pullThreshold: Float,
		private val pullListener: OnPullGestureListener,
	) : EdgeEffect(view.context) {

		private var pullProgressTop: Float = 0f
		private var pullProgressBottom: Float = 0f

		override fun onPull(deltaDistance: Float) {
			val sign = if (direction == DIRECTION_TOP) 1f else if (direction == DIRECTION_BOTTOM) 1f else 0f
			if (sign != 0f) onPull(deltaDistance, 0.5f)
		}

		override fun onPull(deltaDistance: Float, displacement: Float) {
			if (direction == DIRECTION_TOP) {
				pullProgressTop = (pullProgressTop + deltaDistance).coerceAtLeast(0f)
				pullListener.onPullProgressTop(pullProgressTop / pullThreshold)
			} else if (direction == DIRECTION_BOTTOM) {
				pullProgressBottom = (pullProgressBottom + deltaDistance).coerceAtLeast(0f)
				pullListener.onPullProgressBottom(pullProgressBottom / pullThreshold)
			}
		}

		override fun onRelease() {
			var triggered = false
			if (direction == DIRECTION_TOP) {
				if (pullProgressTop >= pullThreshold) {
					pullListener.onPullTriggeredTop()
					triggered = true
				}
				pullProgressTop = 0f
				pullListener.onPullProgressTop(0f)
			} else if (direction == DIRECTION_BOTTOM) {
				if (pullProgressBottom >= pullThreshold) {
					pullListener.onPullTriggeredBottom()
					triggered = true
				}
				pullProgressBottom = 0f
				pullListener.onPullProgressBottom(0f)
			}
			if (!triggered) {
				pullListener.onPullCancelled()
			}
		}

		override fun draw(canvas: Canvas?): Boolean = false

		class Factory : EdgeEffectFactory() {

			override fun createEdgeEffect(view: RecyclerView, direction: Int): EdgeEffect {
				val pullListener = (view as? WebtoonRecyclerView)?.pullListener
				return if (pullListener != null) {
					PullEffect(view, direction, view.pullThreshold, pullListener)
				} else {
					super.createEdgeEffect(view, direction)
				}
			}
		}
	}

	interface OnWebtoonScrollListener {

		fun onScrollChanged(
			recyclerView: WebtoonRecyclerView,
			dy: Int,
			firstVisiblePosition: Int,
			lastVisiblePosition: Int,
		)
	}

	interface OnPullGestureListener {
		fun onPullProgressTop(progress: Float)
		fun onPullProgressBottom(progress: Float)
		fun onPullTriggeredTop()
		fun onPullTriggeredBottom()
		fun onPullCancelled()
	}
}
