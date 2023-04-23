package org.koitharu.kotatsu.base.ui.list

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

abstract class BoundsScrollListener(private val offsetTop: Int, private val offsetBottom: Int) :
	RecyclerView.OnScrollListener() {

	override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
		super.onScrolled(recyclerView, dx, dy)
		val layoutManager = (recyclerView.layoutManager as? LinearLayoutManager) ?: return
		val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
		if (firstVisibleItemPosition == RecyclerView.NO_POSITION) {
			return
		}
		if (firstVisibleItemPosition <= offsetTop) {
			onScrolledToStart(recyclerView)
		}
		val visibleItemCount = layoutManager.childCount
		val totalItemCount = layoutManager.itemCount
		if (visibleItemCount + firstVisibleItemPosition >= totalItemCount - offsetBottom) {
			onScrolledToEnd(recyclerView)
		}
	}

	abstract fun onScrolledToStart(recyclerView: RecyclerView)

	abstract fun onScrolledToEnd(recyclerView: RecyclerView)
}
