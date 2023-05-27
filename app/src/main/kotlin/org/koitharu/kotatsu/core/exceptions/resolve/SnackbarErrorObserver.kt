package org.koitharu.kotatsu.core.exceptions.resolve

import android.view.View
import androidx.core.util.Consumer
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.dialog.ErrorDetailsDialog
import org.koitharu.kotatsu.core.util.ext.getDisplayMessage
import org.koitharu.kotatsu.main.ui.owners.BottomNavOwner
import org.koitharu.kotatsu.parsers.exception.ParseException

class SnackbarErrorObserver(
	host: View,
	fragment: Fragment?,
	resolver: ExceptionResolver?,
	onResolved: Consumer<Boolean>?,
) : ErrorObserver(host, fragment, resolver, onResolved) {

	constructor(
		host: View,
		fragment: Fragment?,
	) : this(host, fragment, null, null)

	override suspend fun emit(value: Throwable) {
		val snackbar = Snackbar.make(host, value.getDisplayMessage(host.context.resources), Snackbar.LENGTH_SHORT)
		if (activity is BottomNavOwner) {
			snackbar.anchorView = activity.bottomNav
		}
		if (canResolve(value)) {
			snackbar.setAction(ExceptionResolver.getResolveStringId(value)) {
				resolve(value)
			}
		} else if (value is ParseException) {
			val fm = fragmentManager
			if (fm != null) {
				snackbar.setAction(R.string.details) {
					ErrorDetailsDialog.show(fm, value, value.url)
				}
			}
		}
		snackbar.show()
	}
}
