package org.koitharu.kotatsu.details.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.view.ActionMode
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.Insets
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.domain.MangaIntent
import org.koitharu.kotatsu.base.ui.BaseActivity
import org.koitharu.kotatsu.browser.BrowserActivity
import org.koitharu.kotatsu.core.exceptions.resolve.ExceptionResolver
import org.koitharu.kotatsu.core.model.parcelable.ParcelableManga
import org.koitharu.kotatsu.core.os.ShortcutsRepository
import org.koitharu.kotatsu.databinding.ActivityDetailsBinding
import org.koitharu.kotatsu.details.ui.adapter.BranchesAdapter
import org.koitharu.kotatsu.download.ui.service.DownloadService
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.util.mapNotNullToSet
import org.koitharu.kotatsu.reader.ui.ReaderActivity
import org.koitharu.kotatsu.reader.ui.ReaderState
import org.koitharu.kotatsu.search.ui.global.GlobalSearchActivity
import org.koitharu.kotatsu.utils.ShareHelper
import org.koitharu.kotatsu.utils.ext.getDisplayMessage

class DetailsActivity :
	BaseActivity<ActivityDetailsBinding>(),
	TabLayoutMediator.TabConfigurationStrategy,
	AdapterView.OnItemSelectedListener {

	private val viewModel by viewModel<DetailsViewModel> {
		parametersOf(MangaIntent(intent))
	}

	private val downloadReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context?, intent: Intent?) {
			val downloadedManga = DownloadService.getDownloadedManga(intent) ?: return
			viewModel.onDownloadComplete(downloadedManga)
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityDetailsBinding.inflate(layoutInflater))
		supportActionBar?.run {
			setDisplayHomeAsUpEnabled(true)
			setDisplayShowTitleEnabled(false)
		}
		val pager = binding.pager
		if (pager != null) {
			pager.adapter = MangaDetailsAdapter(this)
			TabLayoutMediator(checkNotNull(binding.tabs), pager, this).attach()
		}
		gcFragments()
		binding.spinnerBranches?.let(::initSpinner)

		viewModel.manga.observe(this, ::onMangaUpdated)
		viewModel.newChaptersCount.observe(this, ::onNewChaptersChanged)
		viewModel.onMangaRemoved.observe(this, ::onMangaRemoved)
		viewModel.onError.observe(this, ::onError)
		viewModel.onShowToast.observe(this) {
			binding.snackbar.show(messageText = getString(it), longDuration = false)
		}

		registerReceiver(downloadReceiver, IntentFilter(DownloadService.ACTION_DOWNLOAD_COMPLETE))
	}

	override fun onDestroy() {
		unregisterReceiver(downloadReceiver)
		super.onDestroy()
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
			ExceptionResolver.canResolve(e) -> {
				resolveError(e)
			}
			viewModel.manga.value == null -> {
				Toast.makeText(this, e.getDisplayMessage(resources), Toast.LENGTH_LONG).show()
				finishAfterTransition()
			}
			else -> {
				binding.snackbar.show(e.getDisplayMessage(resources))
			}
		}
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		binding.snackbar.updatePadding(
			bottom = insets.bottom
		)
		binding.toolbar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
			topMargin = insets.top
		}
		binding.root.updatePadding(
			left = insets.left,
			right = insets.right
		)
	}

	private fun onNewChaptersChanged(newChapters: Int) {
		val tab = binding.tabs?.getTabAt(1) ?: return
		if (newChapters == 0) {
			tab.removeBadge()
		} else {
			val badge = tab.orCreateBadge
			badge.number = newChapters
			badge.isVisible = true
		}
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
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
					ShareHelper(this).shareCbz(listOf(it.url.toUri().toFile()))
				} else {
					ShareHelper(this).shareMangaLink(it)
				}
			}
			true
		}
		R.id.action_delete -> {
			val title = viewModel.manga.value?.title.orEmpty()
			MaterialAlertDialogBuilder(this)
				.setTitle(R.string.delete_manga)
				.setMessage(getString(R.string.text_delete_local_manga, title))
				.setPositiveButton(R.string.delete) { _, _ ->
					viewModel.deleteLocal()
				}
				.setNegativeButton(android.R.string.cancel, null)
				.show()
			true
		}
		R.id.action_save -> {
			viewModel.manga.value?.let {
				val chaptersCount = it.chapters?.size ?: 0
				val branches = viewModel.branches.value?.toList().orEmpty()
				if (chaptersCount > 5 || branches.size > 1) {
					showSaveConfirmation(it, chaptersCount, branches)
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
						binding.snackbar.show(getString(R.string.operation_not_supported))
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
		binding.pager?.isUserInputEnabled = false
	}

	override fun onSupportActionModeFinished(mode: ActionMode) {
		super.onSupportActionModeFinished(mode)
		binding.pager?.isUserInputEnabled = true
	}

	override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
		val spinner = binding.spinnerBranches ?: return
		viewModel.setSelectedBranch(spinner.selectedItem as String?)
	}

	override fun onNothingSelected(parent: AdapterView<*>?) = Unit

	fun showChapterMissingDialog(chapterId: Long) {
		val remoteManga = viewModel.getRemoteManga()
		if (remoteManga == null) {
			binding.snackbar.show(getString(R.string.chapter_is_missing))
			return
		}
		MaterialAlertDialogBuilder(this).apply {
			setMessage(R.string.chapter_is_missing_text)
			setTitle(R.string.chapter_is_missing)
			setNegativeButton(android.R.string.cancel, null)
			setPositiveButton(R.string.read) { _, _ ->
				startActivity(
					ReaderActivity.newIntent(
						context = this@DetailsActivity,
						manga = remoteManga,
						state = ReaderState(chapterId, 0, 0)
					)
				)
			}
			setNeutralButton(R.string.download) { _, _ ->
				DownloadService.start(this@DetailsActivity, remoteManga, setOf(chapterId))
			}
			setCancelable(true)
		}.show()
	}

	private fun initSpinner(spinner: Spinner) {
		val branchesAdapter = BranchesAdapter()
		spinner.adapter = branchesAdapter
		spinner.onItemSelectedListener = this
		viewModel.branches.observe(this) {
			branchesAdapter.setItems(it)
			spinner.isVisible = it.size > 1
		}
		viewModel.selectedBranchIndex.observe(this) {
			if (it != -1 && it != spinner.selectedItemPosition) {
				spinner.setSelection(it)
			}
		}
	}

	private fun resolveError(e: Throwable) {
		lifecycleScope.launch {
			if (exceptionResolver.resolve(e)) {
				viewModel.reload()
			} else if (viewModel.manga.value == null) {
				Toast.makeText(this@DetailsActivity, e.getDisplayMessage(resources), Toast.LENGTH_LONG).show()
				finishAfterTransition()
			}
		}
	}

	private fun gcFragments() {
		val mustHaveId = binding.pager == null
		val fm = supportFragmentManager
		val fragmentsToRemove = fm.fragments.filter { f ->
			(f.id == 0) == mustHaveId
		}
		if (fragmentsToRemove.isEmpty()) {
			return
		}
		fm.commit {
			setReorderingAllowed(true)
			for (f in fragmentsToRemove) {
				remove(f)
			}
		}
	}

	private fun showSaveConfirmation(manga: Manga, chaptersCount: Int, branches: List<String?>) {
		val dialogBuilder = MaterialAlertDialogBuilder(this)
			.setTitle(R.string.save_manga)
			.setNegativeButton(android.R.string.cancel, null)
		if (branches.size > 1) {
			val items = Array(branches.size) { i -> branches[i].orEmpty() }
			val currentBranch = viewModel.selectedBranchIndex.value ?: -1
			val checkedIndices = BooleanArray(branches.size) { i -> i == currentBranch }
			dialogBuilder.setMultiChoiceItems(items, checkedIndices) { _, i, checked ->
				checkedIndices[i] = checked
			}.setPositiveButton(R.string.save) { _, _ ->
				val selectedBranches = branches.filterIndexedTo(HashSet()) { i, _ -> checkedIndices[i] }
				val chaptersIds = manga.chapters?.mapNotNullToSet { c ->
					if (c.branch in selectedBranches) c.id else null
				}
				DownloadService.start(this, manga, chaptersIds)
			}
		} else {
			dialogBuilder.setMessage(
				getString(
					R.string.large_manga_save_confirm,
					resources.getQuantityString(R.plurals.chapters, chaptersCount, chaptersCount)
				)
			).setPositiveButton(R.string.save) { _, _ ->
				DownloadService.start(this, manga)
			}
		}
		dialogBuilder.show()
	}

	companion object {

		fun newIntent(context: Context, manga: Manga): Intent {
			return Intent(context, DetailsActivity::class.java)
				.putExtra(MangaIntent.KEY_MANGA, ParcelableManga(manga, withChapters = true))
		}

		fun newIntent(context: Context, mangaId: Long): Intent {
			return Intent(context, DetailsActivity::class.java)
				.putExtra(MangaIntent.KEY_ID, mangaId)
		}
	}
}