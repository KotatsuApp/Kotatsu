package org.koitharu.kotatsu.base.ui.list

import androidx.recyclerview.widget.RecyclerView

class ScrollListenerInvalidationObserver(
	private val recyclerView: RecyclerView,
	private val scrollListener: RecyclerView.OnScrollListener,
) : RecyclerView.AdapterDataObserver() {

	override fun onChanged() {
		super.onChanged()
		invalidateScroll()
	}

	override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
		super.onItemRangeInserted(positionStart, itemCount)
		invalidateScroll()
	}

	override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
		super.onItemRangeRemoved(positionStart, itemCount)
		invalidateScroll()
	}

	private fun invalidateScroll() {
		recyclerView.post {
			scrollListener.onScrolled(recyclerView, 0, 0)
		}
	}
}
