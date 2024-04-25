package org.koitharu.kotatsu.reader.ui

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import androidx.fragment.app.FragmentActivity
import org.koitharu.kotatsu.R

class ReaderTopMenuProvider(
	private val activity: FragmentActivity,
	private val viewModel: ReaderViewModel,
) : MenuProvider {

	override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
		menuInflater.inflate(R.menu.opt_reader_top, menu)
	}

	override fun onPrepareMenu(menu: Menu) {
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
