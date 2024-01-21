package org.koitharu.kotatsu.download.ui.list

import android.content.Context
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.util.ext.DIALOG_THEME_CENTERED
import org.koitharu.kotatsu.settings.SettingsActivity

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
			R.id.action_cancel_all -> confirmCancelAll()
			R.id.action_remove_completed -> confirmRemoveCompleted()
			R.id.action_settings -> {
				context.startActivity(SettingsActivity.newDownloadsSettingsIntent(context))
			}

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
		MaterialAlertDialogBuilder(context, DIALOG_THEME_CENTERED)
			.setTitle(R.string.cancel_all)
			.setMessage(R.string.cancel_all_downloads_confirm)
			.setIcon(R.drawable.ic_cancel_multiple)
			.setNegativeButton(android.R.string.cancel, null)
			.setPositiveButton(R.string.confirm) { _, _ ->
				viewModel.cancelAll()
			}.show()
	}

	private fun confirmRemoveCompleted() {
		MaterialAlertDialogBuilder(context, DIALOG_THEME_CENTERED)
			.setTitle(R.string.remove_completed)
			.setMessage(R.string.remove_completed_downloads_confirm)
			.setIcon(R.drawable.ic_clear_all)
			.setNegativeButton(android.R.string.cancel, null)
			.setPositiveButton(R.string.clear) { _, _ ->
				viewModel.removeCompleted()
			}.show()
	}
}
