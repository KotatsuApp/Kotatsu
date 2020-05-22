package org.koitharu.kotatsu.ui.main.tracklogs

import android.view.ViewGroup
import org.koitharu.kotatsu.core.model.TrackingLogItem
import org.koitharu.kotatsu.ui.common.list.BaseRecyclerAdapter
import org.koitharu.kotatsu.ui.common.list.BaseViewHolder
import org.koitharu.kotatsu.ui.common.list.OnRecyclerItemClickListener

class FeedAdapter(onItemClickListener: OnRecyclerItemClickListener<TrackingLogItem>? = null) :
	BaseRecyclerAdapter<TrackingLogItem, Unit>(onItemClickListener) {

	override fun onCreateViewHolder(parent: ViewGroup): BaseViewHolder<TrackingLogItem, Unit> {
		return FeedHolder(parent)
	}

	override fun onGetItemId(item: TrackingLogItem) = item.id

	override fun getExtra(item: TrackingLogItem, position: Int) = Unit
}