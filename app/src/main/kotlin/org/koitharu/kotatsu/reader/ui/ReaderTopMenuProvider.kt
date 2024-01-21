package org.koitharu.kotatsu.reader.ui

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import androidx.fragment.app.FragmentActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.util.ext.DIALOG_THEME_CENTERED

class ReaderTopMenuProvider(
	private val activity: FragmentActivity,
	private val viewModel: ReaderViewModel,
) : MenuProvider {

	override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
		menuInflater.inflate(R.menu.opt_reader_top, menu)
	}

	override fun onPrepareMenu(menu: Menu) {
		menu.findItem(R.id.action_incognito)?.isVisible = viewModel.incognitoMode.value
	}

	override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
		return when (menuItem.itemId) {
			R.id.action_chapters -> {
				ChaptersSheet.show(activity.supportFragmentManager)
				true
			}

			R.id.action_incognito -> {
				showIncognitoModeDialog()
				true
			}

			else -> false
		}
	}

	private fun showIncognitoModeDialog() {
		MaterialAlertDialogBuilder(activity, DIALOG_THEME_CENTERED)
			.setIcon(R.drawable.ic_incognito)
			.setTitle(R.string.incognito_mode)
			.setMessage(R.string.incognito_mode_hint)
			.setPositiveButton(R.string.got_it, null)
			.show()
	}
}
