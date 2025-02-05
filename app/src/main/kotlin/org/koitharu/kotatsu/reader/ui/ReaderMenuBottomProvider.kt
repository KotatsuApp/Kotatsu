package org.koitharu.kotatsu.reader.ui

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.core.view.MenuProvider
import androidx.fragment.app.FragmentActivity
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.nav.router
import org.koitharu.kotatsu.core.prefs.ReaderControl
import org.koitharu.kotatsu.reader.ui.config.ReaderConfigSheet

class ReaderMenuBottomProvider(
	private val activity: FragmentActivity,
	private val readerManager: ReaderManager,
	private val screenOrientationHelper: ScreenOrientationHelper,
	private val configCallback: ReaderConfigSheet.Callback,
	private val viewModel: ReaderViewModel,
) : MenuProvider {

	override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
		menuInflater.inflate(R.menu.opt_reader_bottom, menu)
		onPrepareMenu(menu) // fix, not called in toolbar
	}

	override fun onPrepareMenu(menu: Menu) {
		val readerControls = viewModel.readerControls.value
		val hasPages = viewModel.content.value.pages.isNotEmpty()
		val isPagesSheetEnabled = hasPages && ReaderControl.PAGES_SHEET in readerControls
		menu.findItem(R.id.action_pages_thumbs).run {
			isVisible = isPagesSheetEnabled
			if (isPagesSheetEnabled) {
				setIcon(if (viewModel.isPagesSheetEnabled.value) R.drawable.ic_grid else R.drawable.ic_list)
			}
		}
		menu.findItem(R.id.action_screen_rotation).run {
			isVisible = ReaderControl.SCREEN_ROTATION in readerControls
			when {
				!isVisible -> Unit
				!screenOrientationHelper.isAutoRotationEnabled -> {
					setTitle(R.string.rotate_screen)
					setIcon(R.drawable.ic_screen_rotation)
				}

				else -> {
					setTitle(R.string.lock_screen_rotation)
					setIcon(R.drawable.ic_screen_rotation_lock)
				}
			}
		}
		menu.findItem(R.id.action_save_page)?.run {
			isVisible = hasPages && ReaderControl.SAVE_PAGE in readerControls
		}
	}

	override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
		return when (menuItem.itemId) {
			R.id.action_screen_rotation -> {
				toggleScreenRotation()
				true
			}

			R.id.action_save_page -> {
				configCallback.onSavePageClick()
				true
			}

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

	private fun toggleScreenRotation() = with(screenOrientationHelper) {
		if (isAutoRotationEnabled) {
			val newValue = !isLocked
			isLocked = newValue
			Toast.makeText(
				activity,
				if (newValue) {
					R.string.screen_rotation_locked
				} else {
					R.string.screen_rotation_unlocked
				},
				Toast.LENGTH_SHORT,
			).show()
		} else {
			isLandscape = !isLandscape
		}
	}
}
