package org.koitharu.kotatsu.base.ui.util

import android.view.View
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.domain.reverseAsync
import org.koitharu.kotatsu.main.ui.owners.BottomNavOwner
import org.koitharu.kotatsu.utils.ext.findActivity

class ReversibleActionObserver(
	private val snackbarHost: View,
) : Observer<ReversibleAction?> {

	override fun onChanged(action: ReversibleAction?) {
		if (action == null) {
			return
		}
		val handle = action.handle
		val length = if (handle == null) Snackbar.LENGTH_SHORT else Snackbar.LENGTH_LONG
		val snackbar = Snackbar.make(snackbarHost, action.stringResId, length)
		if (handle != null) {
			snackbar.setAction(R.string.undo) { handle.reverseAsync() }
		}
		(snackbarHost.context.findActivity() as? BottomNavOwner)?.let {
			snackbar.anchorView = it.bottomNav
		}
		snackbar.show()
	}
}
