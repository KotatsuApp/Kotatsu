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
import org.koitharu.kotatsu.alternatives.ui.AlternativesActivity
import org.koitharu.kotatsu.browser.BrowserActivity
import org.koitharu.kotatsu.core.model.LocalMangaSource
import org.koitharu.kotatsu.core.model.isLocal
import org.koitharu.kotatsu.core.os.AppShortcutManager
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.util.ShareHelper
import org.koitharu.kotatsu.download.ui.dialog.DownloadOption
import org.koitharu.kotatsu.scrobbling.common.ui.selector.ScrobblingSelectorSheet
import org.koitharu.kotatsu.search.ui.multi.MultiSearchActivity
import org.koitharu.kotatsu.stats.ui.sheet.MangaStatsSheet

class DetailsMenuProvider(
	private val activity: FragmentActivity,
	private val viewModel: DetailsViewModel,
	private val snackbarHost: View,
	private val appShortcutManager: AppShortcutManager,
) : MenuProvider, OnListItemClickListener<DownloadOption> {

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
		when (menuItem.itemId) {
			R.id.action_share -> {
				viewModel.manga.value?.let {
					val shareHelper = ShareHelper(activity)
					if (it.isLocal) {
						shareHelper.shareCbz(listOf(it.url.toUri().toFile()))
					} else {
						shareHelper.shareMangaLink(it)
					}
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
				DownloadDialogHelper(snackbarHost, viewModel).show(this)
			}

			R.id.action_browser -> {
				viewModel.manga.value?.let {
					activity.startActivity(BrowserActivity.newIntent(activity, it.publicUrl, it.source, it.title))
				}
			}

			R.id.action_online -> {
				viewModel.remoteManga.value?.let {
					activity.startActivity(DetailsActivity.newIntent(activity, it))
				}
			}

			R.id.action_related -> {
				viewModel.manga.value?.let {
					activity.startActivity(MultiSearchActivity.newIntent(activity, it.title))
				}
			}

			R.id.action_alternatives -> {
				viewModel.manga.value?.let {
					activity.startActivity(AlternativesActivity.newIntent(activity, it))
				}
			}

			R.id.action_stats -> {
				viewModel.manga.value?.let {
					MangaStatsSheet.show(activity.supportFragmentManager, it)
				}
			}

			R.id.action_scrobbling -> {
				viewModel.manga.value?.let {
					ScrobblingSelectorSheet.show(activity.supportFragmentManager, it, null)
				}
			}

			R.id.action_shortcut -> {
				viewModel.manga.value?.let {
					activity.lifecycleScope.launch {
						if (!appShortcutManager.requestPinShortcut(it)) {
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

	override fun onItemClick(item: DownloadOption, view: View) {
		val chaptersIds: Set<Long>? = when (item) {
			is DownloadOption.WholeManga -> null
			is DownloadOption.SelectionHint -> {
				viewModel.startChaptersSelection()
				return
			}

			else -> item.chaptersIds
		}
		viewModel.download(chaptersIds)
	}
}
