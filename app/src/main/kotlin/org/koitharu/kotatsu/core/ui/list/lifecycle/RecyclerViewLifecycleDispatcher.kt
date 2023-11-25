package org.koitharu.kotatsu.core.ui.list.lifecycle

import androidx.core.view.children
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION

class RecyclerViewLifecycleDispatcher : RecyclerView.OnScrollListener() {

	private var prevFirst = NO_POSITION
	private var prevLast = NO_POSITION

	override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
		super.onScrolled(recyclerView, dx, dy)
		invalidate(recyclerView)
	}

	fun invalidate(recyclerView: RecyclerView) {
		val lm = recyclerView.layoutManager as? LinearLayoutManager ?: return
		val first = lm.findFirstVisibleItemPosition()
		val last = lm.findLastVisibleItemPosition()
		if (first == prevFirst && last == prevLast) {
			return
		}
		prevFirst = first
		prevLast = last
		if (first == NO_POSITION || last == NO_POSITION) {
			return
		}
		for (child in recyclerView.children) {
			val wh = recyclerView.getChildViewHolder(child) ?: continue
			(wh as? LifecycleAwareViewHolder)?.setIsCurrent(wh.absoluteAdapterPosition in first..last)
		}
	}
}
