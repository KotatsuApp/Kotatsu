package org.koitharu.kotatsu.base.ui.list

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PaginationScrollListener(offset: Int, private val callback: Callback) :
	BoundsScrollListener(0, offset) {

	private var lastTotalCount = 0

	override fun onScrolledToStart(recyclerView: RecyclerView) = Unit

	override fun onScrolledToEnd(recyclerView: RecyclerView) {
		val total = (recyclerView.layoutManager as? LinearLayoutManager)?.itemCount ?: return
		if (total > lastTotalCount) {
			lastTotalCount = total
			callback.onRequestMoreItems(total)
		} else if (total < lastTotalCount) {
			lastTotalCount = total
		}
	}

	fun reset() {
		lastTotalCount = 0
	}

	interface Callback {

		fun onRequestMoreItems(offset: Int)

		@Deprecated("Not in use")
		fun getItemsCount(): Int = 0
	}
}