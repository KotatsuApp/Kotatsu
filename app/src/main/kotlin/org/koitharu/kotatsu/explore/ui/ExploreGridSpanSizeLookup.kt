package org.koitharu.kotatsu.explore.ui

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import org.koitharu.kotatsu.explore.ui.adapter.ExploreAdapter
import org.koitharu.kotatsu.list.ui.adapter.ListItemType

class ExploreGridSpanSizeLookup(
	private val adapter: ExploreAdapter,
	private val layoutManager: GridLayoutManager,
) : SpanSizeLookup() {

	override fun getSpanSize(position: Int): Int {
		val itemType = adapter.getItemViewType(position)
		return if (itemType == ListItemType.EXPLORE_SOURCE_GRID.ordinal) 1 else layoutManager.spanCount
	}
}
