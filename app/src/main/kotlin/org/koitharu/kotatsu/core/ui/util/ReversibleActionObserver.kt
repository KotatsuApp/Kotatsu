package org.koitharu.kotatsu.core.ui.util

import android.view.View
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.FlowCollector
import org.koitharu.kotatsu.R

class ReversibleActionObserver(
	private val snackbarHost: View,
	private val snackbarAnchor: View? = null,
) : FlowCollector<ReversibleAction> {

	override suspend fun emit(value: ReversibleAction) {
		val handle = value.handle
		val length = if (handle == null) Snackbar.LENGTH_SHORT else Snackbar.LENGTH_LONG
		val snackbar = Snackbar.make(snackbarHost, value.stringResId, length)
		if (snackbarAnchor?.isShown == true) {
			snackbar.anchorView = snackbarAnchor
		}
		if (handle != null) {
			snackbar.setAction(R.string.undo) { handle.reverseAsync() }
		}
		snackbar.show()
	}
}
