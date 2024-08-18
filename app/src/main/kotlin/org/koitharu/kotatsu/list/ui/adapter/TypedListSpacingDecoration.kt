package org.koitharu.kotatsu.list.ui.adapter

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import org.koitharu.kotatsu.R

class TypedListSpacingDecoration(
	context: Context,
	private val addHorizontalPadding: Boolean,
) : ItemDecoration() {

	private val spacingSmall = context.resources.getDimensionPixelOffset(R.dimen.list_spacing_small)
	private val spacingNormal =
		context.resources.getDimensionPixelOffset(R.dimen.list_spacing_normal)
	private val spacingLarge = context.resources.getDimensionPixelOffset(R.dimen.list_spacing_large)

	override fun getItemOffsets(
		outRect: Rect,
		view: View,
		parent: RecyclerView,
		state: RecyclerView.State,
	) {
		val itemType = parent.getChildViewHolder(view)?.itemViewType?.let {
			ListItemType.entries.getOrNull(it)
		}
		when (itemType) {
			ListItemType.FILTER_SORT,
			ListItemType.FILTER_TAG,
			ListItemType.FILTER_TAG_MULTI,
			ListItemType.FILTER_STATE,
			ListItemType.FILTER_LANGUAGE,
			ListItemType.QUICK_FILTER,
				-> outRect.set(0)

			ListItemType.HEADER,
			ListItemType.FEED,
			ListItemType.EXPLORE_SOURCE_LIST,
			ListItemType.MANGA_SCROBBLING,
			ListItemType.MANGA_LIST,
				-> outRect.set(0)

			ListItemType.DOWNLOAD,
			ListItemType.HINT_EMPTY,
			ListItemType.MANGA_LIST_DETAILED,
				-> outRect.set(spacingNormal)

			ListItemType.PAGE_THUMB -> outRect.set(spacingNormal)
			ListItemType.MANGA_GRID -> outRect.set(0)

			ListItemType.EXPLORE_BUTTONS -> outRect.set(spacingNormal)

			ListItemType.FOOTER_LOADING,
			ListItemType.FOOTER_ERROR,
			ListItemType.STATE_LOADING,
			ListItemType.STATE_ERROR,
			ListItemType.STATE_EMPTY,
			ListItemType.EXPLORE_SOURCE_GRID,
			ListItemType.EXPLORE_SUGGESTION,
			ListItemType.MANGA_NESTED_GROUP,
			ListItemType.CATEGORY_LARGE,
			ListItemType.NAV_ITEM,
			ListItemType.CHAPTER_LIST,
			ListItemType.INFO,
			null,
				-> outRect.set(0)

			ListItemType.CHAPTER_GRID -> outRect.set(spacingSmall)

			ListItemType.TIP -> outRect.set(0) // TODO
		}
		if (addHorizontalPadding && !itemType.isEdgeToEdge()) {
			outRect.set(
				outRect.left + spacingNormal,
				outRect.top,
				outRect.right + spacingNormal,
				outRect.bottom,
			)
		}
	}

	private fun Rect.set(spacing: Int) = set(spacing, spacing, spacing, spacing)

	private fun ListItemType?.isEdgeToEdge() = this == ListItemType.MANGA_NESTED_GROUP
		|| this == ListItemType.FILTER_SORT
		|| this == ListItemType.FILTER_TAG
		|| this == ListItemType.CHAPTER_LIST
		|| this == ListItemType.CHAPTER_GRID
}
