package org.koitharu.kotatsu.utils

import androidx.recyclerview.widget.RecyclerView
import java.lang.ref.WeakReference

class ScrollResetCallback(recyclerView: RecyclerView) : Runnable {

	private val recyclerViewRef = WeakReference(recyclerView)

	override fun run() {
		recyclerViewRef.get()?.scrollToPosition(0)
	}
}