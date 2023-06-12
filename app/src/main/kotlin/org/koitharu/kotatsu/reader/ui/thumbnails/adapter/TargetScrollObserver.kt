package org.koitharu.kotatsu.reader.ui.thumbnails.adapter

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import org.koitharu.kotatsu.reader.ui.thumbnails.PageThumbnail

class TargetScrollObserver(
	private val recyclerView: RecyclerView,
) : RecyclerView.AdapterDataObserver() {

	private var isScrollToCurrentPending = true

	private val layoutManager: LinearLayoutManager
		get() = recyclerView.layoutManager as LinearLayoutManager

	override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
		if (isScrollToCurrentPending) {
			postScroll()
		}
	}

	private fun postScroll() {
		recyclerView.post {
			scrollToTarget()
		}
	}

	private fun scrollToTarget() {
		val adapter = recyclerView.adapter ?: return
		if (recyclerView.computeVerticalScrollRange() == 0) {
			return
		}
		val snapshot = (adapter as? AsyncListDifferDelegationAdapter<*>)?.items ?: return
		val target = snapshot.indexOfFirst { it is PageThumbnail && it.isCurrent }
		if (target in snapshot.indices) {
			layoutManager.scrollToPositionWithOffset(target, 0)
			isScrollToCurrentPending = false
		}
	}
}
