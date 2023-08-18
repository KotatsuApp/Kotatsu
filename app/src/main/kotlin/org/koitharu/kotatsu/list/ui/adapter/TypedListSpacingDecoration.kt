package org.koitharu.kotatsu.list.ui.adapter

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import org.koitharu.kotatsu.R

class TypedListSpacingDecoration(
	context: Context,
) : ItemDecoration() {

	private val spacingList = context.resources.getDimensionPixelOffset(R.dimen.list_spacing)
	private val spacingGrid = context.resources.getDimensionPixelOffset(R.dimen.grid_spacing)
	private val spacingGridTop = context.resources.getDimensionPixelOffset(R.dimen.grid_spacing_top)

	override fun getItemOffsets(
		outRect: Rect,
		view: View,
		parent: RecyclerView,
		state: RecyclerView.State
	) {
		val itemType = parent.getChildViewHolder(view)?.itemViewType?.let {
			ListItemType.entries.getOrNull(it)
		}
		when (itemType) {
			ListItemType.FILTER_SORT,
			ListItemType.FILTER_TAG -> outRect.set(0)

			ListItemType.HEADER -> outRect.set(spacingList, 0, spacingList, 0)

			ListItemType.EXPLORE_SOURCE_LIST,
			ListItemType.MANGA_LIST -> outRect.set(spacingList, 0, spacingList, 0)

			ListItemType.DOWNLOAD,
			ListItemType.MANGA_LIST_DETAILED -> outRect.set(spacingList)

			ListItemType.PAGE_THUMB,
			ListItemType.MANGA_GRID -> outRect.set(spacingGrid)

			ListItemType.FOOTER_LOADING,
			ListItemType.FOOTER_ERROR,
			ListItemType.STATE_LOADING,
			ListItemType.STATE_ERROR,
			ListItemType.STATE_EMPTY,
			ListItemType.EXPLORE_BUTTONS,
			ListItemType.EXPLORE_SOURCE_GRID,
			ListItemType.EXPLORE_SUGGESTION,
			ListItemType.MANGA_NESTED_GROUP,
			null -> outRect.set(0)

			ListItemType.TIP -> outRect.set(0) // TODO
			ListItemType.HINT_EMPTY,
			ListItemType.FEED -> outRect.set(spacingList, 0, spacingList, 0)
		}
	}

	private fun Rect.set(spacing: Int) = set(spacing, spacing, spacing, spacing)
}
