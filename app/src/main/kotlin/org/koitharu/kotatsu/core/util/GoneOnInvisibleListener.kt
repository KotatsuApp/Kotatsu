package org.koitharu.kotatsu.core.util

import android.view.View
import android.view.ViewTreeObserver

/**
 * ProgressIndicator become INVISIBLE instead of GONE by hide() call.
 * It`s final so we need this workaround
 */
class GoneOnInvisibleListener(
	private val view: View,
) : ViewTreeObserver.OnGlobalLayoutListener {

	override fun onGlobalLayout() {
		if (view.visibility == View.INVISIBLE) {
			view.visibility = View.GONE
		}
	}

	fun attach() {
		view.viewTreeObserver.addOnGlobalLayoutListener(this)
		onGlobalLayout()
	}

	fun detach() {
		view.viewTreeObserver.removeOnGlobalLayoutListener(this)
	}
}
