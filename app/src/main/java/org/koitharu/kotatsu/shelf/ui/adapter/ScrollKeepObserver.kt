package org.koitharu.kotatsu.shelf.ui.adapter

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ScrollKeepObserver(
	private val recyclerView: RecyclerView,
) : RecyclerView.AdapterDataObserver() {

	private val layoutManager: LinearLayoutManager
		get() = recyclerView.layoutManager as LinearLayoutManager

	override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
		val position = minOf(toPosition, fromPosition) // if items are swapping positions may be swapped too
		if (position < layoutManager.findFirstVisibleItemPosition()) {
			postScroll(position)
		}
	}

	override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
		if (positionStart < layoutManager.findFirstVisibleItemPosition()) {
			postScroll(positionStart)
		}
	}

	private fun postScroll(targetPosition: Int) {
		recyclerView.post {
			layoutManager.scrollToPositionWithOffset(targetPosition, 0)
		}
	}
}
