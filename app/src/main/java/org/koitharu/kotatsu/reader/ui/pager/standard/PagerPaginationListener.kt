package org.koitharu.kotatsu.reader.ui.pager.standard

import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import org.koitharu.kotatsu.reader.ui.pager.OnBoundsScrollListener

class PagerPaginationListener(
	private val adapter: RecyclerView.Adapter<*>,
	private val offset: Int,
	private val listener: OnBoundsScrollListener
) : ViewPager2.OnPageChangeCallback() {

	private var firstItemId: Long = 0
	private var lastItemId: Long = 0

	override fun onPageSelected(position: Int) {
		val itemCount = adapter.itemCount
		if (itemCount == 0) {
			return
		}
		if (position <= offset && adapter.getItemId(0) != firstItemId) {
			firstItemId = adapter.getItemId(0)
			listener.onScrolledToStart()
		} else if (position >= itemCount - offset && adapter.getItemId(itemCount - 1) != lastItemId) {
			lastItemId = adapter.getItemId(itemCount - 1)
			listener.onScrolledToEnd()
		}
	}
}