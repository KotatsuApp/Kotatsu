package org.koitharu.kotatsu.core.ui.list

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver

class RecyclerScrollKeeper(
	private val rv: RecyclerView,
) : AdapterDataObserver() {

	private val scrollUpRunnable = Runnable {
		(rv.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(0, 0)
	}

	fun attach() {
		rv.adapter?.registerAdapterDataObserver(this)
	}

	override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
		super.onItemRangeInserted(positionStart, itemCount)
		if (positionStart == 0 && isScrolledToTop()) {
			postScrollUp()
		}
	}

	override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
		super.onItemRangeMoved(fromPosition, toPosition, itemCount)
		if (toPosition == 0 && isScrolledToTop()) {
			postScrollUp()
		}
	}

	private fun postScrollUp() {
		rv.postDelayed(scrollUpRunnable, 500L)
	}

	private fun isScrolledToTop(): Boolean {
		return (rv.layoutManager as? LinearLayoutManager)?.findFirstVisibleItemPosition() == 0
	}
}
