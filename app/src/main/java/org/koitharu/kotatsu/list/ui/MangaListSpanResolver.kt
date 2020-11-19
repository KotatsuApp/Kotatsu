package org.koitharu.kotatsu.list.ui

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.list.ui.adapter.MangaListAdapter
import kotlin.math.abs
import kotlin.math.roundToInt

class MangaListSpanResolver(
	context: Context,
	private val adapter: MangaListAdapter
) : GridLayoutManager.SpanSizeLookup(), View.OnLayoutChangeListener {

	private val gridWidth = context.resources.getDimension(R.dimen.preferred_grid_width)
	private var cellWidth = -1f

	override fun getSpanSize(position: Int) = when(adapter.getItemViewType(position)) {
		else -> 1
	}

	override fun onLayoutChange(
		v: View?,
		left: Int,
		top: Int,
		right: Int,
		bottom: Int,
		oldLeft: Int,
		oldTop: Int,
		oldRight: Int,
		oldBottom: Int
	) {
		if (cellWidth <= 0f) {
			return
		}
		val rv = v as? RecyclerView ?: return
		val width = abs(right - left)
		if (width == 0) {
			return
		}
		(rv.layoutManager as? GridLayoutManager)?.spanCount = resolveGridSpanCount(width)
	}

	fun setGridSize(gridSize: Int) {
		val scaleFactor = gridSize / 100f
		cellWidth = gridWidth * scaleFactor
	}

	private fun resolveGridSpanCount(width: Int): Int {
		val estimatedCount = (width / cellWidth).roundToInt()
		return estimatedCount.coerceAtLeast(2)
	}
}