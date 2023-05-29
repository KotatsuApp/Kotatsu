package org.koitharu.kotatsu.core.ui.list.decor

import android.graphics.Rect
import android.view.View
import androidx.annotation.Px
import androidx.recyclerview.widget.RecyclerView

class SpacingItemDecoration(@Px private val spacing: Int) : RecyclerView.ItemDecoration() {

	override fun getItemOffsets(
		outRect: Rect,
		view: View,
		parent: RecyclerView,
		state: RecyclerView.State,
	) {
		outRect.set(spacing, spacing, spacing, spacing)
	}
}
