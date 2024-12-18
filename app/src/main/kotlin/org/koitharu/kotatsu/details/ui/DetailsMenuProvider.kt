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
import org.koitharu.kotatsu.core.model.LocalMangaSource
import org.koitharu.kotatsu.core.model.isLocal
import org.koitharu.kotatsu.core.nav.router
import org.koitharu.kotatsu.core.os.AppShortcutManager
import org.koitharu.kotatsu.core.util.ShareHelper

class DetailsMenuProvider(
	private val activity: FragmentActivity,
	private val viewModel: DetailsViewModel,
	private val snackbarHost: View,
	private val appShortcutManager: AppShortcutManager,
) : MenuProvider {

	override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
		menuInflater.inflate(R.menu.opt_details, menu)
	}

	override fun onPrepareMenu(menu: Menu) {
		val manga = viewModel.manga.value
		menu.findItem(R.id.action_save).isVisible = manga?.source != null && manga.source != LocalMangaSource
		menu.findItem(R.id.action_delete).isVisible = manga?.source == LocalMangaSource
		menu.findItem(R.id.action_browser).isVisible = manga?.source != LocalMangaSource
		menu.findItem(R.id.action_alternatives).isVisible = manga?.source != LocalMangaSource
		menu.findItem(R.id.action_shortcut).isVisible = ShortcutManagerCompat.isRequestPinShortcutSupported(activity)
		menu.findItem(R.id.action_scrobbling).isVisible = viewModel.isScrobblingAvailable
		menu.findItem(R.id.action_online).isVisible = viewModel.remoteManga.value != null
		menu.findItem(R.id.action_stats).isVisible = viewModel.isStatsAvailable.value
	}

	override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
		val manga = viewModel.getMangaOrNull() ?: return false
		when (menuItem.itemId) {
			R.id.action_share -> {
				val shareHelper = ShareHelper(activity)
				if (manga.isLocal) {
					shareHelper.shareCbz(listOf(manga.url.toUri().toFile()))
				} else {
					shareHelper.shareMangaLink(manga)
				}
			}

			R.id.action_delete -> {
				MaterialAlertDialogBuilder(activity)
					.setTitle(R.string.delete_manga)
					.setMessage(activity.getString(R.string.text_delete_local_manga, manga.title))
					.setPositiveButton(R.string.delete) { _, _ ->
						viewModel.deleteLocal()
					}
					.setNegativeButton(android.R.string.cancel, null)
					.show()
			}

			R.id.action_save -> {
				activity.router.showDownloadDialog(manga, snackbarHost)
			}

			R.id.action_browser -> {
				activity.router.openBrowser(url = manga.publicUrl, source = manga.source, title = manga.title)
			}

			R.id.action_online -> {
				activity.router.openDetails(manga)
			}

			R.id.action_related -> {
				activity.router.openSearch(manga.title)
			}

			R.id.action_alternatives -> {
				activity.router.openAlternatives(manga)
			}

			R.id.action_stats -> {
				activity.router.showStatisticSheet(manga)
			}

			R.id.action_scrobbling -> {
				activity.router.showScrobblingSelectorSheet(manga, null)
			}

			R.id.action_shortcut -> {
				activity.lifecycleScope.launch {
					if (!appShortcutManager.requestPinShortcut(manga)) {
						Snackbar.make(snackbarHost, R.string.operation_not_supported, Snackbar.LENGTH_SHORT)
							.show()
					}
				}
			}

			else -> return false
		}
		return true
	}
}
