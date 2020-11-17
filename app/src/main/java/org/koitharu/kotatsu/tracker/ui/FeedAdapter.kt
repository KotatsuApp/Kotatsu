package org.koitharu.kotatsu.tracker.ui

import android.view.ViewGroup
import org.koitharu.kotatsu.base.ui.list.BaseRecyclerAdapter
import org.koitharu.kotatsu.base.ui.list.BaseViewHolder
import org.koitharu.kotatsu.base.ui.list.OnRecyclerItemClickListener
import org.koitharu.kotatsu.core.model.TrackingLogItem

class FeedAdapter(onItemClickListener: OnRecyclerItemClickListener<TrackingLogItem>? = null) :
	BaseRecyclerAdapter<TrackingLogItem, Unit>(onItemClickListener) {

	override fun onCreateViewHolder(parent: ViewGroup): BaseViewHolder<TrackingLogItem, Unit> {
		return FeedHolder(parent)
	}

	override fun onGetItemId(item: TrackingLogItem) = item.id

	override fun getExtra(item: TrackingLogItem, position: Int) = Unit
}