package org.koitharu.kotatsu.download.ui.worker

import android.view.View
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.FlowCollector
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.util.ext.findActivity
import org.koitharu.kotatsu.download.ui.list.DownloadsActivity
import org.koitharu.kotatsu.main.ui.owners.BottomNavOwner

class DownloadStartedObserver(
	private val snackbarHost: View,
) : FlowCollector<Unit> {

	override suspend fun emit(value: Unit) {
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
