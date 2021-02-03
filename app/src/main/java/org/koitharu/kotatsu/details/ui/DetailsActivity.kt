package org.koitharu.kotatsu.details.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.Toolbar
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.Insets
import androidx.core.net.toFile
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.domain.MangaIntent
import org.koitharu.kotatsu.base.ui.BaseActivity
import org.koitharu.kotatsu.browser.BrowserActivity
import org.koitharu.kotatsu.browser.cloudflare.CloudFlareDialog
import org.koitharu.kotatsu.core.exceptions.CloudFlareProtectedException
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.os.ShortcutsRepository
import org.koitharu.kotatsu.databinding.ActivityDetailsBinding
import org.koitharu.kotatsu.download.DownloadService
import org.koitharu.kotatsu.search.ui.global.GlobalSearchActivity
import org.koitharu.kotatsu.utils.ShareHelper
import org.koitharu.kotatsu.utils.ext.getDisplayMessage

class DetailsActivity : BaseActivity<ActivityDetailsBinding>(),
	TabLayoutMediator.TabConfigurationStrategy {

	private val viewModel by viewModel<DetailsViewModel> {
		parametersOf(MangaIntent.from(intent))
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityDetailsBinding.inflate(layoutInflater))
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		binding.pager.adapter = MangaDetailsAdapter(this)
		TabLayoutMediator(binding.tabs, binding.pager, this).attach()

		viewModel.manga.observe(this, ::onMangaUpdated)
		viewModel.newChaptersCount.observe(this, ::onNewChaptersChanged)
		viewModel.onMangaRemoved.observe(this, ::onMangaRemoved)
		viewModel.onError.observe(this, ::onError)
	}

	private fun onMangaUpdated(manga: Manga) {
		title = manga.title
		invalidateOptionsMenu()
	}

	private fun onMangaRemoved(manga: Manga) {
		Toast.makeText(
			this, getString(R.string._s_deleted_from_local_storage, manga.title),
			Toast.LENGTH_SHORT
		).show()
		finishAfterTransition()
	}

	private fun onError(e: Throwable) {
		when {
			e is CloudFlareProtectedException -> {
				CloudFlareDialog.newInstance(e.url)
					.show(supportFragmentManager, CloudFlareDialog.TAG)
			}
			viewModel.manga.value == null -> {
				Toast.makeText(this, e.getDisplayMessage(resources), Toast.LENGTH_LONG).show()
				finishAfterTransition()
			}
			else -> {
				Snackbar.make(binding.pager, e.getDisplayMessage(resources), Snackbar.LENGTH_LONG)
					.show()
			}
		}
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		binding.toolbar.updatePadding(
			top = insets.top,
			left = insets.left,
			right = insets.right
		)
		if (binding.tabs.parent !is Toolbar) {
			binding.tabs.updatePadding(
				left = insets.left,
				right = insets.right
			)
		}
	}

	private fun onNewChaptersChanged(newChapters: Int) {
		val tab = binding.tabs.getTabAt(1) ?: return
		if (newChapters == 0) {
			tab.removeBadge()
		} else {
			val badge = tab.orCreateBadge
			badge.number = newChapters
			badge.isVisible = true
		}
	}

	override fun onCreateOptionsMenu(menu: Menu?): Boolean {
		menuInflater.inflate(R.menu.opt_details, menu)
		return super.onCreateOptionsMenu(menu)
	}

	override fun onPrepareOptionsMenu(menu: Menu): Boolean {
		val manga = viewModel.manga.value
		menu.findItem(R.id.action_save).isVisible =
			manga?.source != null && manga.source != MangaSource.LOCAL
		menu.findItem(R.id.action_delete).isVisible =
			manga?.source == MangaSource.LOCAL
		menu.findItem(R.id.action_browser).isVisible =
			manga?.source != MangaSource.LOCAL
		menu.findItem(R.id.action_shortcut).isVisible =
			ShortcutManagerCompat.isRequestPinShortcutSupported(this)
		return super.onPrepareOptionsMenu(menu)
	}

	override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
		R.id.action_share -> {
			viewModel.manga.value?.let {
				if (it.source == MangaSource.LOCAL) {
					ShareHelper(this).shareCbz(Uri.parse(it.url).toFile())
				} else {
					ShareHelper(this).shareMangaLink(it)
				}
			}
			true
		}
		R.id.action_delete -> {
			viewModel.manga.value?.let { m ->
				AlertDialog.Builder(this)
					.setTitle(R.string.delete_manga)
					.setMessage(getString(R.string.text_delete_local_manga, m.title))
					.setPositiveButton(R.string.delete) { _, _ ->
						viewModel.deleteLocal(m)
					}
					.setNegativeButton(android.R.string.cancel, null)
					.show()
			}
			true
		}
		R.id.action_save -> {
			viewModel.manga.value?.let {
				val chaptersCount = it.chapters?.size ?: 0
				if (chaptersCount > 5) {
					AlertDialog.Builder(this)
						.setTitle(R.string.save_manga)
						.setMessage(
							getString(
								R.string.large_manga_save_confirm,
								resources.getQuantityString(
									R.plurals.chapters,
									chaptersCount,
									chaptersCount
								)
							)
						)
						.setNegativeButton(android.R.string.cancel, null)
						.setPositiveButton(R.string.save) { _, _ ->
							DownloadService.start(this, it)
						}.show()
				} else {
					DownloadService.start(this, it)
				}
			}
			true
		}
		R.id.action_browser -> {
			viewModel.manga.value?.let {
				startActivity(BrowserActivity.newIntent(this, it.publicUrl, it.title))
			}
			true
		}
		R.id.action_related -> {
			viewModel.manga.value?.let {
				startActivity(GlobalSearchActivity.newIntent(this, it.title))
			}
			true
		}
		R.id.action_shortcut -> {
			viewModel.manga.value?.let {
				lifecycleScope.launch {
					if (!get<ShortcutsRepository>().requestPinShortcut(it)) {
						Snackbar.make(
							binding.pager,
							R.string.operation_not_supported,
							Snackbar.LENGTH_SHORT
						).show()
					}
				}
			}
			true
		}
		else -> super.onOptionsItemSelected(item)
	}

	override fun onConfigureTab(tab: TabLayout.Tab, position: Int) {
		tab.text = when (position) {
			0 -> getString(R.string.details)
			1 -> getString(R.string.chapters)
			else -> null
		}
	}

	override fun onSupportActionModeStarted(mode: ActionMode) {
		super.onSupportActionModeStarted(mode)
		binding.pager.isUserInputEnabled = false
	}

	override fun onSupportActionModeFinished(mode: ActionMode) {
		super.onSupportActionModeFinished(mode)
		binding.pager.isUserInputEnabled = true
	}

	companion object {

		const val ACTION_MANGA_VIEW = "${BuildConfig.APPLICATION_ID}.action.VIEW_MANGA"

		fun newIntent(context: Context, manga: Manga): Intent {
			return Intent(context, DetailsActivity::class.java)
				.putExtra(MangaIntent.KEY_MANGA, manga)
		}

		fun newIntent(context: Context, mangaId: Long): Intent {
			return Intent(context, DetailsActivity::class.java)
				.putExtra(MangaIntent.KEY_ID, mangaId)
		}
	}
}