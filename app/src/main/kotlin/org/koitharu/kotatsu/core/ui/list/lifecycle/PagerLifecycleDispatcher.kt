package org.koitharu.kotatsu.core.ui.list.lifecycle

import androidx.core.view.children
import androidx.viewpager2.widget.ViewPager2
import org.koitharu.kotatsu.core.util.ext.recyclerView

class PagerLifecycleDispatcher(
	private val pager: ViewPager2,
) : ViewPager2.OnPageChangeCallback() {

	override fun onPageSelected(position: Int) {
		super.onPageSelected(position)
		val rv = pager.recyclerView ?: return
		for (child in rv.children) {
			val wh = rv.getChildViewHolder(child) ?: continue
			(wh as? LifecycleAwareViewHolder)?.setIsCurrent(wh.absoluteAdapterPosition == position)
		}
	}

	fun invalidate() {
		onPageSelected(pager.currentItem)
	}
}
