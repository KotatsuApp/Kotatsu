package org.koitharu.kotatsu.reader.ui

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import androidx.fragment.app.FragmentActivity
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.reader.ui.config.ReaderConfigSheet
import org.koitharu.kotatsu.reader.ui.thumbnails.PagesThumbnailsSheet
import org.koitharu.kotatsu.settings.SettingsActivity

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
		menu.findItem(R.id.action_pages_thumbs).isVisible = hasPages

		val bookmarkItem = menu.findItem(R.id.action_bookmark) ?: return
		bookmarkItem.isVisible = hasPages
		if (hasPages) {
			val hasBookmark = viewModel.isBookmarkAdded.value
			bookmarkItem.setTitle(if (hasBookmark) R.string.bookmark_remove else R.string.bookmark_add)
			bookmarkItem.setIcon(if (hasBookmark) R.drawable.ic_bookmark_added else R.drawable.ic_bookmark)
		}
	}

	override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
		return when (menuItem.itemId) {
			R.id.action_settings -> {
				activity.startActivity(SettingsActivity.newReaderSettingsIntent(activity))
				true
			}

			R.id.action_pages_thumbs -> {
				val state = viewModel.getCurrentState() ?: return false
				PagesThumbnailsSheet.show(
					activity.supportFragmentManager,
					viewModel.manga?.toManga() ?: return false,
					state.chapterId,
					state.page,
				)
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
