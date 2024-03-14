package org.koitharu.kotatsu.details.ui.pager.chapters

import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.list.ui.adapter.ListItemType
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

	class SpanSizeLookup(
		private val recyclerView: RecyclerView
	) : GridLayoutManager.SpanSizeLookup() {

		override fun getSpanSize(position: Int): Int {
			return when (recyclerView.adapter?.getItemViewType(position)) {
				ListItemType.CHAPTER_LIST.ordinal, // for smooth transition
				ListItemType.HEADER.ordinal -> getTotalSpans()

				else -> 1
			}
		}

		private fun getTotalSpans() = (recyclerView.layoutManager as? GridLayoutManager)?.spanCount ?: 1
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
