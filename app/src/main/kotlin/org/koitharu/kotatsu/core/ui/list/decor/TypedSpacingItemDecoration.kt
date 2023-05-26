package org.koitharu.kotatsu.core.ui.list.decor

import android.graphics.Rect
import android.util.SparseIntArray
import android.view.View
import androidx.core.util.getOrDefault
import androidx.core.util.set
import androidx.recyclerview.widget.RecyclerView

class TypedSpacingItemDecoration(
	vararg spacingMapping: Pair<Int, Int>,
	private val fallbackSpacing: Int = 0,
) : RecyclerView.ItemDecoration() {

	private val mapping = SparseIntArray(spacingMapping.size)

	init {
		spacingMapping.forEach { (k, v) -> mapping[k] = v }
	}

	override fun getItemOffsets(
		outRect: Rect,
		view: View,
		parent: RecyclerView,
		state: RecyclerView.State
	) {
		val itemType = parent.getChildViewHolder(view)?.itemViewType
		val spacing = if (itemType == null) {
			fallbackSpacing
		} else {
			mapping.getOrDefault(itemType, fallbackSpacing)
		}
		outRect.set(spacing, spacing, spacing, spacing)
	}
}
