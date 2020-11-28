package org.koitharu.kotatsu.base.ui.list.decor

import android.graphics.Rect
import android.view.View
import androidx.annotation.Px
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SpacingItemDecoration(@Px private val spacing: Int) : RecyclerView.ItemDecoration() {

	private val halfSpacing = spacing / 2

	override fun getItemOffsets(
		outRect: Rect,
		view: View,
		parent: RecyclerView,
		state: RecyclerView.State
	) {
		val spans = (parent.layoutManager as? GridLayoutManager)?.spanCount ?: 1
		val position = parent.getChildAdapterPosition(view)
		outRect.set(
			if (position % spans == 0) spacing else halfSpacing,
			if (position < spans) spacing else halfSpacing,
			if ((position + 1) % spans == 0) spacing else halfSpacing,
			spacing //TODO check bottom
		)
	}
}