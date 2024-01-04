package org.koitharu.kotatsu.core.ui.list

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

abstract class BoundsScrollListener(
	@JvmField protected val offsetTop: Int,
	@JvmField protected val offsetBottom: Int
) : RecyclerView.OnScrollListener() {

	override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
		super.onScrolled(recyclerView, dx, dy)
		if (recyclerView.hasPendingAdapterUpdates()) {
			return
		}
		val layoutManager = (recyclerView.layoutManager as? LinearLayoutManager) ?: return
		val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
		if (firstVisibleItemPosition == RecyclerView.NO_POSITION) {
			return
		}
		val visibleItemCount = layoutManager.childCount
		val totalItemCount = layoutManager.itemCount
		if (visibleItemCount + firstVisibleItemPosition >= totalItemCount - offsetBottom) {
			onScrolledToEnd(recyclerView)
		}
		if (firstVisibleItemPosition <= offsetTop) {
			onScrolledToStart(recyclerView)
		}
		onPostScrolled(recyclerView, firstVisibleItemPosition, visibleItemCount)
	}

	abstract fun onScrolledToStart(recyclerView: RecyclerView)

	abstract fun onScrolledToEnd(recyclerView: RecyclerView)

	protected open fun onPostScrolled(
		recyclerView: RecyclerView,
		firstVisibleItemPosition: Int,
		visibleItemCount: Int
	) = Unit

	fun invalidate(recyclerView: RecyclerView) {
		onScrolled(recyclerView, 0, 0)
	}

	fun postInvalidate(recyclerView: RecyclerView) = recyclerView.post {
		invalidate(recyclerView)
	}
}
