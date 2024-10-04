package org.koitharu.kotatsu.core.ui.dialog

import android.content.Context
import androidx.annotation.UiContext
import org.koitharu.kotatsu.R

object CommonAlertDialogs {

	fun showDownloadConfirmation(
		@UiContext context: Context,
		onConfirmed: (startPaused: Boolean) -> Unit,
	) = buildAlertDialog(context, isCentered = true) {
		var startPaused = false
		setTitle(R.string.save_manga)
		setIcon(R.drawable.ic_download)
		setMessage(R.string.save_manga_confirm)
		setCheckbox(R.string.start_download, true) { _, isChecked ->
			startPaused = !isChecked
		}
		setPositiveButton(R.string.save) { _, _ ->
			onConfirmed(startPaused)
		}
		setNegativeButton(android.R.string.cancel, null)
	}.show()
}
