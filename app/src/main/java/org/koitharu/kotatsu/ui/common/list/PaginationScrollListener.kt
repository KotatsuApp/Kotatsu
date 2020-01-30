package org.koitharu.kotatsu.ui.common.list

import androidx.recyclerview.widget.RecyclerView

class PaginationScrollListener(offset: Int, private val callback: Callback) : BoundsScrollListener(0, offset) {

	private var lastTotalCount = 0

	override fun onScrolledToTop(recyclerView: RecyclerView)  = Unit

	override fun onScrolledToEnd(recyclerView: RecyclerView) {
		val total = recyclerView.adapter?.itemCount ?: 0
		if (total > lastTotalCount) {
			callback.onRequestMoreItems(total)
			lastTotalCount = total
		} else if (total < lastTotalCount) {
			lastTotalCount = total
		}
	}

	interface Callback {

		fun onRequestMoreItems(offset: Int)
	}
}