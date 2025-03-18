package org.koitharu.kotatsu.download.ui.list

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import androidx.fragment.app.FragmentActivity
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.nav.router
import org.koitharu.kotatsu.core.ui.dialog.buildAlertDialog

class DownloadsMenuProvider(
	private val activity: FragmentActivity,
	private val viewModel: DownloadsViewModel,
) : MenuProvider {

	override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
		menuInflater.inflate(R.menu.opt_downloads, menu)
	}

	override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
		when (menuItem.itemId) {
			R.id.action_pause -> viewModel.pauseAll()
			R.id.action_resume -> viewModel.resumeAll()
			R.id.action_cancel_all -> confirmCancelAll()
			R.id.action_remove_completed -> confirmRemoveCompleted()
			R.id.action_settings -> activity.router.openDownloadsSetting()
			else -> return false
		}
		return true
	}

	override fun onPrepareMenu(menu: Menu) {
		super.onPrepareMenu(menu)
		menu.findItem(R.id.action_pause)?.isVisible = viewModel.hasActiveWorks.value == true
		menu.findItem(R.id.action_resume)?.isVisible = viewModel.hasPausedWorks.value == true
		menu.findItem(R.id.action_cancel_all)?.isVisible = viewModel.hasCancellableWorks.value == true
	}

	private fun confirmCancelAll() {
		buildAlertDialog(activity, isCentered = true) {
			setTitle(R.string.cancel_all)
			setMessage(R.string.cancel_all_downloads_confirm)
			setIcon(R.drawable.ic_cancel_multiple)
			setNegativeButton(android.R.string.cancel, null)
			setPositiveButton(R.string.confirm) { _, _ -> viewModel.cancelAll() }
		}.show()
	}

	private fun confirmRemoveCompleted() {
		buildAlertDialog(activity, isCentered = true) {
			setTitle(R.string.remove_completed)
			setMessage(R.string.remove_completed_downloads_confirm)
			setIcon(R.drawable.ic_clear_all)
			setNegativeButton(android.R.string.cancel, null)
			setPositiveButton(R.string.clear) { _, _ -> viewModel.removeCompleted() }
		}.show()
	}
}
