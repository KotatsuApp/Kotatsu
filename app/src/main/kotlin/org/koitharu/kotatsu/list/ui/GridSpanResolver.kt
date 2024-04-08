package org.koitharu.kotatsu.list.ui

import android.content.res.Resources
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.koitharu.kotatsu.R
import kotlin.math.abs
import kotlin.math.roundToInt

class GridSpanResolver(
	resources: Resources,
) : View.OnLayoutChangeListener {

	var spanCount = 3
		private set

	private val gridWidth = resources.getDimension(R.dimen.preferred_grid_width)
	private val spacing = resources.getDimension(R.dimen.grid_spacing)
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
		oldBottom: Int,
	) {
		if (cellWidth <= 0f) {
			return
		}
		val rv = v as? RecyclerView ?: return
		val width = abs(right - left)
		if (width == 0) {
			return
		}
		resolveGridSpanCount(width)
		(rv.layoutManager as? GridLayoutManager)?.spanCount = spanCount
	}

	fun setGridSize(scaleFactor: Float, rv: RecyclerView) {
		cellWidth = (gridWidth * scaleFactor) + spacing
		val lm = rv.layoutManager as? GridLayoutManager ?: return
		val innerWidth = lm.width - lm.paddingEnd - lm.paddingStart
		if (innerWidth > 0) {
			resolveGridSpanCount(innerWidth)
			lm.spanCount = spanCount
		}
	}

	private fun resolveGridSpanCount(width: Int) {
		val estimatedCount = (width / cellWidth).roundToInt()
		spanCount = estimatedCount.coerceAtLeast(2)
	}
}
