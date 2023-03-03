package org.koitharu.kotatsu.core.exceptions.resolve

import android.view.View
import androidx.core.util.Consumer
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.MangaErrorDialog
import org.koitharu.kotatsu.main.ui.owners.BottomNavOwner
import org.koitharu.kotatsu.parsers.exception.ParseException
import org.koitharu.kotatsu.utils.ext.getDisplayMessage

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

	override fun onChanged(error: Throwable?) {
		if (error == null) {
			return
		}
		val snackbar = Snackbar.make(host, error.getDisplayMessage(host.context.resources), Snackbar.LENGTH_SHORT)
		if (activity is BottomNavOwner) {
			snackbar.anchorView = activity.bottomNav
		}
		if (canResolve(error)) {
			snackbar.setAction(ExceptionResolver.getResolveStringId(error)) {
				resolve(error)
			}
		} else if (error is ParseException) {
			val fm = fragmentManager
			if (fm != null) {
				snackbar.setAction(R.string.details) {
					MangaErrorDialog.show(fm, error)
				}
			}
		}
		snackbar.show()
	}
}
