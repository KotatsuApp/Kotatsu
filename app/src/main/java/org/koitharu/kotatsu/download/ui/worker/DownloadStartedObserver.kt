package org.koitharu.kotatsu.download.ui.worker

import android.view.View
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.download.ui.DownloadsActivity
import org.koitharu.kotatsu.main.ui.owners.BottomNavOwner
import org.koitharu.kotatsu.utils.ext.findActivity

class DownloadStartedObserver(
	private val snackbarHost: View,
) : Observer<Unit> {

	override fun onChanged(value: Unit) {
		val snackbar = Snackbar.make(snackbarHost, R.string.download_started, Snackbar.LENGTH_LONG)
		(snackbarHost.context.findActivity() as? BottomNavOwner)?.let {
			snackbar.anchorView = it.bottomNav
		}
		snackbar.setAction(R.string.details) {
			it.context.startActivity(DownloadsActivity.newIntent(it.context))
		}
		snackbar.show()
	}
}
