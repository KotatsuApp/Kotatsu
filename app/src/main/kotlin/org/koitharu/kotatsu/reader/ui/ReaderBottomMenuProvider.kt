package org.koitharu.kotatsu.reader.ui

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import androidx.fragment.app.FragmentActivity
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.details.ui.pager.ChaptersPagesSheet
import org.koitharu.kotatsu.reader.ui.config.ReaderConfigSheet

class ReaderBottomMenuProvider(
	private val activity: FragmentActivity,
	private val readerManager: ReaderManager,
	private val viewModel: ReaderViewModel,
) : MenuProvider {

	override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
		menuInflater.inflate(R.menu.opt_reader_bottom, menu)
		onPrepareMenu(menu) // fix, not called in toolbar
	}

	override fun onPrepareMenu(menu: Menu) {
		val hasPages = viewModel.content.value.pages.isNotEmpty()
		menu.findItem(R.id.action_pages_thumbs).run {
			isVisible = hasPages
			if (hasPages) {
				setIcon(if (viewModel.isPagesSheetEnabled.value) R.drawable.ic_grid else R.drawable.ic_list)
			}
		}
	}

	override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
		return when (menuItem.itemId) {
			R.id.action_pages_thumbs -> {
				ChaptersPagesSheet.show(activity.supportFragmentManager)
				true
			}

			R.id.action_options -> {
				viewModel.saveCurrentState(readerManager.currentReader?.getCurrentState())
				val currentMode = readerManager.currentMode ?: return false
				ReaderConfigSheet.show(activity.supportFragmentManager, currentMode)
				true
			}

			else -> false
		}
	}
}
