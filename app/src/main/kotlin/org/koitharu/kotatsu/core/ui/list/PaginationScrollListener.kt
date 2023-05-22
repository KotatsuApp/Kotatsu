package org.koitharu.kotatsu.core.ui.list

import androidx.recyclerview.widget.RecyclerView

class PaginationScrollListener(offset: Int, private val callback: Callback) :
	BoundsScrollListener(0, offset) {

	override fun onScrolledToStart(recyclerView: RecyclerView) = Unit

	override fun onScrolledToEnd(recyclerView: RecyclerView) {
		callback.onScrolledToEnd()
	}

	interface Callback {

		fun onScrolledToEnd()
	}
}
