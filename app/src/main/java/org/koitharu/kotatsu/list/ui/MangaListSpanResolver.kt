package org.koitharu.kotatsu.list.ui

import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.koitharu.kotatsu.R
import kotlin.math.abs
import kotlin.math.roundToInt

class MangaListSpanResolver : View.OnLayoutChangeListener {

	var spanCount = 3
		private set

	private var gridWidth = -1f
	private var cellWidth = -1f

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
		if (gridWidth < 0f) {
			gridWidth = rv.resources.getDimension(R.dimen.preferred_grid_width)
		}
		val width = abs(right - left)
		if (width == 0) {
			return
		}
		resolveGridSpanCount(width)
		(rv.layoutManager as? GridLayoutManager)?.spanCount = spanCount
	}

	fun setGridSize(scaleFactor: Float, rv: RecyclerView?) {
		if (gridWidth < 0f) {
			gridWidth = (rv ?: return).resources.getDimension(R.dimen.preferred_grid_width)
		}
		cellWidth = gridWidth * scaleFactor
		if (rv != null) {
			val width = rv.width
			if (width != 0) {
				resolveGridSpanCount(width)
				(rv.layoutManager as? GridLayoutManager)?.spanCount = spanCount
			}
		}
	}

	private fun resolveGridSpanCount(width: Int) {
		val estimatedCount = (width / cellWidth).roundToInt()
		spanCount = estimatedCount.coerceAtLeast(2)
	}
}