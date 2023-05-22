package org.koitharu.kotatsu.core.util.ext

import androidx.recyclerview.widget.LinearLayoutManager
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

fun RecyclerView.findCenterViewPosition(): Int {
	val centerX = width / 2f
	val centerY = height / 2f
	val view = findChildViewUnder(centerX, centerY) ?: return RecyclerView.NO_POSITION
	return getChildAdapterPosition(view)
}

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
		if (childCount == 0) {
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
