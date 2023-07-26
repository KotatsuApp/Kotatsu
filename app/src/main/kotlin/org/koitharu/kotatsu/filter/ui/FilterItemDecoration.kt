package org.koitharu.kotatsu.filter.ui

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import org.koitharu.kotatsu.R

class FilterItemDecoration(
	context: Context,
) : RecyclerView.ItemDecoration() {

	private val spacing = context.resources.getDimensionPixelOffset(R.dimen.list_spacing)

	override fun getItemOffsets(
		outRect: Rect,
		view: View,
		parent: RecyclerView,
		state: RecyclerView.State
	) {
		val itemType = parent.getChildViewHolder(view)?.itemViewType ?: -1
		if (itemType == FilterAdapter.ITEM_TYPE_HEADER) {
			outRect.set(spacing, 0, spacing, 0)
		} else {
			outRect.set(0, 0, 0, 0)
		}
	}
}
