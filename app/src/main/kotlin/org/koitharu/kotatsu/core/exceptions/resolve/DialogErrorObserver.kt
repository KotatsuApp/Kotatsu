package org.koitharu.kotatsu.core.exceptions.resolve

import android.content.DialogInterface
import android.view.View
import androidx.core.util.Consumer
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.dialog.ErrorDetailsDialog
import org.koitharu.kotatsu.core.util.ext.getDisplayMessage
import org.koitharu.kotatsu.parsers.exception.ParseException

class DialogErrorObserver(
	host: View,
	fragment: Fragment?,
	resolver: ExceptionResolver?,
	private val onResolved: Consumer<Boolean>?,
) : ErrorObserver(host, fragment, resolver, onResolved) {

	constructor(
		host: View,
		fragment: Fragment?,
	) : this(host, fragment, null, null)

	override suspend fun emit(value: Throwable) {
		val listener = DialogListener(value)
		val dialogBuilder = MaterialAlertDialogBuilder(activity ?: host.context)
			.setMessage(value.getDisplayMessage(host.context.resources))
			.setNegativeButton(R.string.close, listener)
			.setOnCancelListener(listener)
		if (canResolve(value)) {
			dialogBuilder.setPositiveButton(ExceptionResolver.getResolveStringId(value), listener)
		} else if (value is ParseException) {
			val fm = fragmentManager
			if (fm != null) {
				dialogBuilder.setPositiveButton(R.string.details) { _, _ ->
					ErrorDetailsDialog.show(fm, value, value.url)
				}
			}
		}
		val dialog = dialogBuilder.create()
		if (activity != null) {
			dialog.setOwnerActivity(activity)
		}
		dialog.show()
	}

	private inner class DialogListener(
		private val error: Throwable,
	) : DialogInterface.OnClickListener, DialogInterface.OnCancelListener {

		override fun onClick(dialog: DialogInterface?, which: Int) {
			when (which) {
				DialogInterface.BUTTON_NEGATIVE -> onResolved?.accept(false)
				DialogInterface.BUTTON_POSITIVE -> resolve(error)
			}
		}

		override fun onCancel(dialog: DialogInterface?) {
			onResolved?.accept(false)
		}
	}
}
