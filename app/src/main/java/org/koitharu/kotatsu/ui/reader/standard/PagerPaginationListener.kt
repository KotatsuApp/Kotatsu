package org.koitharu.kotatsu.ui.reader.standard

import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import org.koitharu.kotatsu.ui.reader.OnBoundsScrollListener

class PagerPaginationListener(
	private val adapter: RecyclerView.Adapter<*>,
	private val offset: Int,
	private val listener: OnBoundsScrollListener
) : ViewPager2.OnPageChangeCallback() {

	private var lastItemCountStart = 0
	private var lastItemCountEnd = 0

	override fun onPageSelected(position: Int) {
		val itemCount = adapter.itemCount
		if (position <= offset && itemCount != lastItemCountStart) {
			lastItemCountStart = itemCount
			listener.onScrolledToStart()
		} else if (position >= itemCount - offset && itemCount != lastItemCountEnd) {
			lastItemCountEnd = itemCount
			listener.onScrolledToEnd()
		}
	}
}