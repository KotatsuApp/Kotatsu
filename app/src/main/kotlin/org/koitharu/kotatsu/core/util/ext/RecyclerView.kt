package org.koitharu.kotatsu.core.util.ext

import android.util.DisplayMetrics
import androidx.core.view.doOnNextLayout
import androidx.core.view.isEmpty
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.hannesdorfmann.adapterdelegates4.dsl.AdapterDelegateViewBindingViewHolder
import com.hannesdorfmann.adapterdelegates4.dsl.AdapterDelegateViewHolder

fun RecyclerView.clearItemDecorations() {
	suppressLayout(true)
	while (itemDecorationCount > 0) {
		removeItemDecorationAt(0)
	}
	suppressLayout(false)
}

fun RecyclerView.removeItemDecoration(cls: Class<out RecyclerView.ItemDecoration>) {
	repeat(itemDecorationCount) { i ->
		if (cls.isInstance(getItemDecorationAt(i))) {
			removeItemDecorationAt(i)
			return
		}
	}
}

var RecyclerView.firstVisibleItemPosition: Int
	get() = (layoutManager as? LinearLayoutManager)?.findFirstVisibleItemPosition()
		?: RecyclerView.NO_POSITION
	set(value) {
		if (value != RecyclerView.NO_POSITION) {
			(layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(value, 0)
		}
	}

val RecyclerView.visibleItemCount: Int
	get() = (layoutManager as? LinearLayoutManager)?.run {
		findLastVisibleItemPosition() - findFirstVisibleItemPosition()
	} ?: 0

fun <T> RecyclerView.ViewHolder.getItem(clazz: Class<T>): T? {
	val rawItem = when (this) {
		is AdapterDelegateViewBindingViewHolder<*, *> -> item
		is AdapterDelegateViewHolder<*> -> item
		else -> null
	} ?: return null
	return if (clazz.isAssignableFrom(rawItem.javaClass)) {
		clazz.cast(rawItem)
	} else {
		null
	}
}

val RecyclerView.isScrolledToTop: Boolean
	get() {
		if (isEmpty()) {
			return true
		}
		val holder = findViewHolderForAdapterPosition(0)
		return holder != null && holder.itemView.top >= 0
	}

val RecyclerView.LayoutManager?.firstVisibleItemPosition
	get() = when (this) {
		is LinearLayoutManager -> findFirstVisibleItemPosition()
		is StaggeredGridLayoutManager -> findFirstVisibleItemPositions(null)[0]
		else -> 0
	}

val RecyclerView.LayoutManager?.isLayoutReversed
	get() = when (this) {
		is LinearLayoutManager -> reverseLayout
		is StaggeredGridLayoutManager -> reverseLayout
		else -> false
	}

// https://medium.com/flat-pack-tech/quickly-scroll-to-the-top-of-a-recyclerview-da15b717f3c4
fun RecyclerView.smoothScrollToTop() {
	val layoutManager = layoutManager as? LinearLayoutManager ?: return

	if (!context.isAnimationsEnabled) {
		layoutManager.scrollToPositionWithOffset(0, 0)
		return
	}

	val smoothScroller = object : LinearSmoothScroller(context) {
		init {
			targetPosition = 0
		}

		override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics?) =
			super.calculateSpeedPerPixel(displayMetrics) / DEFAULT_SPEED_FACTOR
	}

	val jumpBeforeScroll = layoutManager.findFirstVisibleItemPosition() > DEFAULT_JUMP_THRESHOLD
	if (jumpBeforeScroll) {
		layoutManager.scrollToPositionWithOffset(DEFAULT_JUMP_THRESHOLD, 0)
		doOnNextLayout {
			layoutManager.startSmoothScroll(smoothScroller)
		}
	} else {
		layoutManager.startSmoothScroll(smoothScroller)
	}
}

private const val DEFAULT_JUMP_THRESHOLD = 30
private const val DEFAULT_SPEED_FACTOR = 1f
