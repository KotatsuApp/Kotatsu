package org.koitharu.kotatsu.core.ui.dialog

import android.content.Context
import android.content.DialogInterface
import androidx.annotation.UiContext
import androidx.core.net.ConnectivityManagerCompat
import dagger.Lazy
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.TriStateOption
import org.koitharu.kotatsu.core.util.ext.connectivityManager
import javax.inject.Inject

class CommonAlertDialogs @Inject constructor(
	private val settings: Lazy<AppSettings>,
) {

	fun askForDownloadOverMeteredNetwork(
		@UiContext context: Context,
		onConfirmed: (allow: Boolean) -> Unit
	) {
		when (settings.get().allowDownloadOnMeteredNetwork) {
			TriStateOption.ENABLED -> onConfirmed(true)
			TriStateOption.DISABLED -> onConfirmed(false)
			TriStateOption.ASK -> {
				if (!ConnectivityManagerCompat.isActiveNetworkMetered(context.connectivityManager)) {
					onConfirmed(true)
					return
				}
				val listener = DialogInterface.OnClickListener { _, which ->
					when (which) {
						DialogInterface.BUTTON_POSITIVE -> {
							settings.get().allowDownloadOnMeteredNetwork = TriStateOption.ENABLED
							onConfirmed(true)
						}

						DialogInterface.BUTTON_NEUTRAL -> {
							onConfirmed(true)
						}

						DialogInterface.BUTTON_NEGATIVE -> {
							settings.get().allowDownloadOnMeteredNetwork = TriStateOption.DISABLED
							onConfirmed(false)
						}
					}
				}
				BigButtonsAlertDialog.Builder(context)
					.setIcon(R.drawable.ic_network_cellular)
					.setTitle(R.string.download_cellular_confirm)
					.setPositiveButton(R.string.allow_always, listener)
					.setNeutralButton(R.string.allow_once, listener)
					.setNegativeButton(R.string.dont_allow, listener)
					.create()
					.show()
			}
		}
	}
}
