package org.koitharu.kotatsu.details.ui

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.core.view.MenuProvider
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.browser.BrowserActivity
import org.koitharu.kotatsu.core.os.ShortcutsUpdater
import org.koitharu.kotatsu.core.util.ShareHelper
import org.koitharu.kotatsu.details.ui.model.MangaBranch
import org.koitharu.kotatsu.favourites.ui.categories.select.FavouriteCategoriesBottomSheet
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.util.mapNotNullToSet
import org.koitharu.kotatsu.scrobbling.common.ui.selector.ScrobblingSelectorBottomSheet
import org.koitharu.kotatsu.search.ui.multi.MultiSearchActivity

class DetailsMenuProvider(
	private val activity: FragmentActivity,
	private val viewModel: DetailsViewModel,
	private val snackbarHost: View,
	private val shortcutsUpdater: ShortcutsUpdater,
) : MenuProvider {

	override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
		menuInflater.inflate(R.menu.opt_details, menu)
	}

	override fun onPrepareMenu(menu: Menu) {
		val manga = viewModel.manga.value
		menu.findItem(R.id.action_save).isVisible = manga?.source != null && manga.source != MangaSource.LOCAL
		menu.findItem(R.id.action_delete).isVisible = manga?.source == MangaSource.LOCAL
		menu.findItem(R.id.action_browser).isVisible = manga?.source != MangaSource.LOCAL
		menu.findItem(R.id.action_shortcut).isVisible = ShortcutManagerCompat.isRequestPinShortcutSupported(activity)
		menu.findItem(R.id.action_scrobbling).isVisible = viewModel.isScrobblingAvailable
		menu.findItem(R.id.action_favourite).setIcon(
			if (viewModel.favouriteCategories.value == true) R.drawable.ic_heart else R.drawable.ic_heart_outline,
		)
	}

	override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
		when (menuItem.itemId) {
			R.id.action_share -> {
				viewModel.manga.value?.let {
					val shareHelper = ShareHelper(activity)
					if (it.source == MangaSource.LOCAL) {
						shareHelper.shareCbz(listOf(it.url.toUri().toFile()))
					} else {
						shareHelper.shareMangaLink(it)
					}
				}
			}

			R.id.action_favourite -> {
				viewModel.manga.value?.let {
					FavouriteCategoriesBottomSheet.show(activity.supportFragmentManager, it)
				}
			}

			R.id.action_delete -> {
				val title = viewModel.manga.value?.title.orEmpty()
				MaterialAlertDialogBuilder(activity)
					.setTitle(R.string.delete_manga)
					.setMessage(activity.getString(R.string.text_delete_local_manga, title))
					.setPositiveButton(R.string.delete) { _, _ ->
						viewModel.deleteLocal()
					}
					.setNegativeButton(android.R.string.cancel, null)
					.show()
			}

			R.id.action_save -> {
				viewModel.manga.value?.let {
					val chaptersCount = it.chapters?.size ?: 0
					val branches = viewModel.branches.value.orEmpty()
					if (chaptersCount > 5 || branches.size > 1) {
						showSaveConfirmation(it, chaptersCount, branches)
					} else {
						viewModel.download(null)
					}
				}
			}

			R.id.action_browser -> {
				viewModel.manga.value?.let {
					activity.startActivity(BrowserActivity.newIntent(activity, it.publicUrl, it.title))
				}
			}

			R.id.action_related -> {
				viewModel.manga.value?.let {
					activity.startActivity(MultiSearchActivity.newIntent(activity, it.title))
				}
			}

			R.id.action_scrobbling -> {
				viewModel.manga.value?.let {
					ScrobblingSelectorBottomSheet.show(activity.supportFragmentManager, it, null)
				}
			}

			R.id.action_shortcut -> {
				viewModel.manga.value?.let {
					activity.lifecycleScope.launch {
						if (!shortcutsUpdater.requestPinShortcut(it)) {
							Snackbar.make(snackbarHost, R.string.operation_not_supported, Snackbar.LENGTH_SHORT)
								.show()
						}
					}
				}
			}

			else -> return false
		}
		return true
	}

	private fun showSaveConfirmation(manga: Manga, chaptersCount: Int, branches: List<MangaBranch>) {
		val dialogBuilder = MaterialAlertDialogBuilder(activity)
			.setTitle(R.string.save_manga)
			.setNegativeButton(android.R.string.cancel, null)
		if (branches.size > 1) {
			val items = Array(branches.size) { i -> branches[i].name.orEmpty() }
			val currentBranch = branches.indexOfFirst { it.isSelected }
			val checkedIndices = BooleanArray(branches.size) { i -> i == currentBranch }
			dialogBuilder.setMultiChoiceItems(items, checkedIndices) { _, i, checked ->
				checkedIndices[i] = checked
			}.setPositiveButton(R.string.save) { _, _ ->
				val selectedBranches = branches.mapIndexedNotNullTo(HashSet()) { i, b ->
					if (checkedIndices[i]) b.name else null
				}
				val chaptersIds = manga.chapters?.mapNotNullToSet { c ->
					if (c.branch in selectedBranches) c.id else null
				}
				viewModel.download(chaptersIds)
			}
		} else {
			dialogBuilder.setMessage(
				activity.getString(
					R.string.large_manga_save_confirm,
					activity.resources.getQuantityString(R.plurals.chapters, chaptersCount, chaptersCount),
				),
			).setPositiveButton(R.string.save) { _, _ ->
				viewModel.download(null)
			}
		}
		dialogBuilder.show()
	}
}
