package org.koitharu.kotatsu.base.ui.util

import android.view.View
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.domain.reverseAsync

class ReversibleActionObserver(
	private val snackbarHost: View,
) : Observer<ReversibleAction?> {

	override fun onChanged(value: ReversibleAction?) {
		if (value == null) {
			return
		}
		val handle = value.handle
		val length = if (handle == null) Snackbar.LENGTH_SHORT else Snackbar.LENGTH_LONG
		val snackbar = Snackbar.make(snackbarHost, value.stringResId, length)
		if (handle != null) {
			snackbar.setAction(R.string.undo) { handle.reverseAsync() }
		}
		snackbar.show()
	}
}
