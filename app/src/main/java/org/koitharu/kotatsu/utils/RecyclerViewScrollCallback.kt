package org.koitharu.kotatsu.utils

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.lang.ref.WeakReference

class RecyclerViewScrollCallback(recyclerView: RecyclerView, private val position: Int) : Runnable {

	private val recyclerViewRef = WeakReference(recyclerView)

	override fun run() {
		val rv = recyclerViewRef.get() ?: return
		val lm = rv.layoutManager ?: return
		rv.stopScroll()
		if (lm is LinearLayoutManager) {
			lm.scrollToPositionWithOffset(position, 0)
		} else {
			lm.scrollToPosition(position)
		}
	}
}