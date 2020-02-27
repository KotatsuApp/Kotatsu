package org.koitharu.kotatsu.ui.reader.wetoon

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.koitharu.kotatsu.ui.reader.OnBoundsScrollListener

class ListPaginationListener(
	private val offset: Int,
	private val listener: OnBoundsScrollListener
) : RecyclerView.OnScrollListener() {

	private var lastItemCountStart = 0
	private var lastItemCountEnd = 0

	override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
		super.onScrolled(recyclerView, dx, dy)
		val itemCount = recyclerView.adapter?.itemCount ?: return
		val layoutManager = (recyclerView.layoutManager as? LinearLayoutManager) ?: return
		val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
		val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()
		if (firstVisiblePosition <= offset && itemCount != lastItemCountStart) {
			lastItemCountStart = itemCount
			listener.onScrolledToStart()
		} else if (lastVisiblePosition >= itemCount - offset && itemCount != lastItemCountEnd) {
			lastItemCountEnd = itemCount
			listener.onScrolledToEnd()
		}
	}
}