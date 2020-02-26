package org.koitharu.kotatsu.ui.common.list

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

abstract class BoundsScrollListener(private val offsetTop: Int, private val offsetBottom: Int) :
	RecyclerView.OnScrollListener() {

	constructor(offset: Int = 0) : this(offset, offset)

	override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
		super.onScrolled(recyclerView, dx, dy)
		val layoutManager = (recyclerView.layoutManager as? LinearLayoutManager) ?: return
		val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
		if (firstVisibleItemPosition <= offsetTop) {
			onScrolledToStart(recyclerView)
			return
		}
		val visibleItemCount = layoutManager.childCount
		val totalItemCount = layoutManager.itemCount
		if (visibleItemCount + firstVisibleItemPosition >= totalItemCount - offsetBottom && firstVisibleItemPosition >= 0) {
			onScrolledToEnd(recyclerView)
		}
	}

	abstract fun onScrolledToStart(recyclerView: RecyclerView)

	abstract fun onScrolledToEnd(recyclerView: RecyclerView)
}