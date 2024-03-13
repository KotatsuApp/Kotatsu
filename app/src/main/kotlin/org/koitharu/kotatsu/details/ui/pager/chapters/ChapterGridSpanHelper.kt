package org.koitharu.kotatsu.details.ui.pager.chapters

import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.koitharu.kotatsu.R
import kotlin.math.roundToInt

class ChapterGridSpanHelper private constructor() : View.OnLayoutChangeListener {

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
		val rv = v as? RecyclerView ?: return
		if (rv.width > 0) {
			apply(rv)
		}
	}

	private fun apply(rv: RecyclerView) {
		(rv.layoutManager as? GridLayoutManager)?.spanCount = getSpanCount(rv)
	}

	companion object {

		fun attach(view: RecyclerView) {
			val helper = ChapterGridSpanHelper()
			view.addOnLayoutChangeListener(helper)
			helper.apply(view)
		}

		fun getSpanCount(view: RecyclerView): Int {
			val cellWidth = view.resources.getDimension(R.dimen.chapter_grid_width)
			val estimatedCount = (view.width / cellWidth).roundToInt()
			return estimatedCount.coerceAtLeast(2)
		}
	}
}
