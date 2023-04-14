package org.koitharu.kotatsu.shelf.ui.adapter

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ScrollKeepObserver(
	private val recyclerView: RecyclerView,
) : RecyclerView.AdapterDataObserver() {

	private val layoutManager: LinearLayoutManager
		get() = recyclerView.layoutManager as LinearLayoutManager

	override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
		val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
		val position = minOf(toPosition, fromPosition) // if items are swapping positions may be swapped too
		if (firstVisiblePosition != RecyclerView.NO_POSITION && (position == 0 || position < firstVisiblePosition)) {
			postScroll(position)
		}
	}

	override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
		val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
		if (firstVisiblePosition != RecyclerView.NO_POSITION && (positionStart == 0 || positionStart < firstVisiblePosition)) {
			postScroll(positionStart)
		}
	}

	private fun postScroll(targetPosition: Int) {
		recyclerView.post {
			layoutManager.scrollToPositionWithOffset(targetPosition, 0)
		}
	}
}
