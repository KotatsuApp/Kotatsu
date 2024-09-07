package org.koitharu.kotatsu.core.ui.list.lifecycle

import android.view.View
import androidx.core.view.children
import androidx.viewpager2.widget.ViewPager2
import org.koitharu.kotatsu.core.util.ext.recyclerView

class PagerLifecycleDispatcher(
	private val pager: ViewPager2,
) : ViewPager2.OnPageChangeCallback() {

	private var pendingUpdate: OneShotLayoutListener? = null

	override fun onPageSelected(position: Int) {
		setResumedPage(position)
	}

	fun invalidate() {
		setResumedPage(pager.currentItem)
	}

	fun postInvalidate() = pager.post {
		invalidate()
	}

	private fun setResumedPage(position: Int) {
		pendingUpdate?.cancel()
		pendingUpdate = null
		var hasResumedItem = false
		val rv = pager.recyclerView ?: return
		if (rv.childCount == 0) {
			return
		}
		for (child in rv.children) {
			val wh = rv.getChildViewHolder(child) ?: continue
			val isCurrent = wh.absoluteAdapterPosition == position
			(wh as? LifecycleAwareViewHolder)?.setIsCurrent(isCurrent)
			if (isCurrent) {
				hasResumedItem = true
			}
		}
		if (!hasResumedItem) {
			rv.addOnLayoutChangeListener(OneShotLayoutListener(rv, position).also { pendingUpdate = it })
		}
	}

	private inner class OneShotLayoutListener(
		private val view: View,
		private val targetPosition: Int,
	) : View.OnLayoutChangeListener {

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
			view.removeOnLayoutChangeListener(this)
			setResumedPage(targetPosition)
		}

		fun cancel() {
			view.removeOnLayoutChangeListener(this)
		}
	}
}
