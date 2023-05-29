package org.koitharu.kotatsu.core.util

import androidx.annotation.Px
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.lang.ref.WeakReference

class RecyclerViewScrollCallback(
	recyclerView: RecyclerView,
	private val position: Int,
	@Px private val offset: Int,
) : Runnable {

	private val recyclerViewRef = WeakReference(recyclerView)

	override fun run() {
		val rv = recyclerViewRef.get() ?: return
		val lm = rv.layoutManager ?: return
		rv.stopScroll()
		if (lm is LinearLayoutManager) {
			lm.scrollToPositionWithOffset(position, offset)
		} else {
			lm.scrollToPosition(position)
		}
	}
}
