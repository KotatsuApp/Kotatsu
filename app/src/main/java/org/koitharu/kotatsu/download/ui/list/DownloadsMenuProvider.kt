package org.koitharu.kotatsu.download.ui.list

import android.content.Context
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import org.koitharu.kotatsu.R

class DownloadsMenuProvider(
	private val context: Context,
	private val viewModel: DownloadsViewModel,
) : MenuProvider {

	override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
		menuInflater.inflate(R.menu.opt_downloads, menu)
	}

	override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
		when (menuItem.itemId) {
			R.id.action_pause -> viewModel.pauseAll()
			R.id.action_resume -> viewModel.resumeAll()
			R.id.action_cancel_all -> viewModel.cancelAll()
			R.id.action_remove_completed -> viewModel.removeCompleted()
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
}
