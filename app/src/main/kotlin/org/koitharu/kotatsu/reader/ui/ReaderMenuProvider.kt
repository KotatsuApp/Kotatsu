package org.koitharu.kotatsu.reader.ui

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import androidx.fragment.app.FragmentActivity
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.nav.router

class ReaderMenuProvider(
	private val activity: FragmentActivity,
	private val readerManager: ReaderManager,
	private val viewModel: ReaderViewModel,
) : MenuProvider {

	override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
		menuInflater.inflate(R.menu.opt_reader, menu)
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
		menu.findItem(R.id.action_bookmark)?.let { bookmarkItem ->
			val hasPages = viewModel.content.value.pages.isNotEmpty()
			bookmarkItem.isEnabled = hasPages
			if (hasPages) {
				val hasBookmark = viewModel.isBookmarkAdded.value
				bookmarkItem.setTitle(if (hasBookmark) R.string.bookmark_remove else R.string.bookmark_add)
				bookmarkItem.setIcon(if (hasBookmark) R.drawable.ic_bookmark_added else R.drawable.ic_bookmark)
			}
		}
	}

	override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
		return when (menuItem.itemId) {
			R.id.action_pages_thumbs -> {
				activity.router.showChapterPagesSheet()
				true
			}

			R.id.action_options -> {
				viewModel.saveCurrentState(readerManager.currentReader?.getCurrentState())
				val currentMode = readerManager.currentMode ?: return false
				activity.router.showReaderConfigSheet(currentMode)
				true
			}

			R.id.action_slider -> {
				viewModel.setSliderVisibility(!viewModel.isSliderVisible.value)
				true
			}

			R.id.action_bookmark -> {
				if (viewModel.isBookmarkAdded.value) {
					viewModel.removeBookmark()
				} else {
					viewModel.addBookmark()
				}
				true
			}

			else -> false
		}
	}
}
