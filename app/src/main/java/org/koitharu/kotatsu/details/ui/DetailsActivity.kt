package org.koitharu.kotatsu.details.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.transition.Slide
import android.transition.TransitionManager
import android.view.Gravity
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.graphics.Insets
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.domain.MangaIntent
import org.koitharu.kotatsu.base.ui.BaseActivity
import org.koitharu.kotatsu.base.ui.widgets.BottomSheetHeaderBar
import org.koitharu.kotatsu.core.exceptions.resolve.ExceptionResolver
import org.koitharu.kotatsu.core.model.parcelable.ParcelableManga
import org.koitharu.kotatsu.core.os.ShortcutsUpdater
import org.koitharu.kotatsu.core.ui.MangaErrorDialog
import org.koitharu.kotatsu.databinding.ActivityDetailsBinding
import org.koitharu.kotatsu.details.service.MangaPrefetchService
import org.koitharu.kotatsu.details.ui.model.ChapterListItem
import org.koitharu.kotatsu.details.ui.model.HistoryInfo
import org.koitharu.kotatsu.download.ui.service.DownloadService
import org.koitharu.kotatsu.main.ui.owners.NoModalBottomSheetOwner
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.reader.ui.ReaderActivity
import org.koitharu.kotatsu.reader.ui.ReaderState
import org.koitharu.kotatsu.utils.ViewBadge
import org.koitharu.kotatsu.utils.ext.assistedViewModels
import org.koitharu.kotatsu.utils.ext.getDisplayMessage
import org.koitharu.kotatsu.utils.ext.isReportable
import org.koitharu.kotatsu.utils.ext.setNavigationBarTransparentCompat
import org.koitharu.kotatsu.utils.ext.textAndVisible
import javax.inject.Inject

@AndroidEntryPoint
class DetailsActivity :
	BaseActivity<ActivityDetailsBinding>(),
	View.OnClickListener,
	BottomSheetHeaderBar.OnExpansionChangeListener,
	NoModalBottomSheetOwner {

	override val bsHeader: BottomSheetHeaderBar?
		get() = binding.headerChapters

	@Inject
	lateinit var viewModelFactory: DetailsViewModel.Factory

	@Inject
	lateinit var shortcutsUpdater: ShortcutsUpdater

	private lateinit var viewBadge: ViewBadge

	private val viewModel: DetailsViewModel by assistedViewModels {
		viewModelFactory.create(MangaIntent(intent))
	}
	private lateinit var chaptersMenuProvider: ChaptersMenuProvider

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
		binding.buttonRead.setOnClickListener(this)
		binding.buttonDropdown.setOnClickListener(this)
		viewBadge = ViewBadge(binding.buttonRead, this)

		chaptersMenuProvider = if (binding.layoutBottom != null) {
			val bsMediator = ChaptersBottomSheetMediator(checkNotNull(binding.layoutBottom))
			actionModeDelegate.addListener(bsMediator)
			checkNotNull(binding.headerChapters).addOnExpansionChangeListener(bsMediator)
			checkNotNull(binding.headerChapters).addOnLayoutChangeListener(bsMediator)
			onBackPressedDispatcher.addCallback(bsMediator)
			ChaptersMenuProvider(viewModel, bsMediator)
		} else {
			ChaptersMenuProvider(viewModel, null)
		}

		viewModel.manga.observe(this, ::onMangaUpdated)
		viewModel.newChaptersCount.observe(this, ::onNewChaptersChanged)
		viewModel.onMangaRemoved.observe(this, ::onMangaRemoved)
		viewModel.onError.observe(this, ::onError)
		viewModel.onShowToast.observe(this) {
			makeSnackbar(getString(it), Snackbar.LENGTH_SHORT).show()
		}
		viewModel.historyInfo.observe(this, ::onHistoryChanged)
		viewModel.selectedBranchName.observe(this) {
			binding.headerChapters?.subtitle = it
			binding.textViewSubtitle?.textAndVisible = it
		}
		viewModel.isChaptersReversed.observe(this) {
			binding.headerChapters?.invalidateMenu() ?: invalidateOptionsMenu()
		}
		viewModel.favouriteCategories.observe(this) {
			invalidateOptionsMenu()
		}
		viewModel.branches.observe(this) {
			binding.buttonDropdown.isVisible = it.size > 1
		}
		viewModel.chapters.observe(this, PrefetchObserver(this))

		registerReceiver(downloadReceiver, IntentFilter(DownloadService.ACTION_DOWNLOAD_COMPLETE))
		addMenuProvider(
			DetailsMenuProvider(
				activity = this,
				viewModel = viewModel,
				snackbarHost = binding.containerChapters,
				shortcutsUpdater = shortcutsUpdater,
			),
		)
		binding.headerChapters?.addOnExpansionChangeListener(this) ?: addMenuProvider(chaptersMenuProvider)
	}

	override fun onDestroy() {
		unregisterReceiver(downloadReceiver)
		super.onDestroy()
	}

	override fun onClick(v: View) {
		val manga = viewModel.manga.value ?: return
		when (v.id) {
			R.id.button_read -> {
				val chapterId = viewModel.historyInfo.value?.history?.chapterId
				if (chapterId != null && manga.chapters?.none { x -> x.id == chapterId } == true) {
					showChapterMissingDialog(chapterId)
				} else {
					startActivity(
						ReaderActivity.newIntent(
							context = this,
							manga = manga,
							branch = viewModel.selectedBranchValue,
						),
					)
				}
			}

			R.id.button_dropdown -> showBranchPopupMenu()
		}
	}

	override fun onExpansionStateChanged(headerBar: BottomSheetHeaderBar, isExpanded: Boolean) {
		if (isExpanded) {
			headerBar.addMenuProvider(chaptersMenuProvider)
		} else {
			headerBar.removeMenuProvider(chaptersMenuProvider)
		}
		binding.buttonRead.isGone = isExpanded
	}

	private fun onMangaUpdated(manga: Manga) {
		title = manga.title
		val hasChapters = !manga.chapters.isNullOrEmpty()
		binding.buttonRead.isEnabled = hasChapters
		invalidateOptionsMenu()
		showBottomSheet(manga.chapters != null)
		binding.groupHeader?.isVisible = hasChapters
	}

	private fun onMangaRemoved(manga: Manga) {
		Toast.makeText(
			this,
			getString(R.string._s_deleted_from_local_storage, manga.title),
			Toast.LENGTH_SHORT,
		).show()
		finishAfterTransition()
	}

	private fun onError(e: Throwable) {
		val manga = viewModel.manga.value
		when {
			ExceptionResolver.canResolve(e) -> {
				resolveError(e)
			}

			manga == null -> {
				Toast.makeText(this, e.getDisplayMessage(resources), Toast.LENGTH_LONG).show()
				finishAfterTransition()
			}

			else -> {
				val snackbar = makeSnackbar(
					e.getDisplayMessage(resources),
					if (viewModel.manga.value?.chapters == null) {
						Snackbar.LENGTH_INDEFINITE
					} else {
						Snackbar.LENGTH_LONG
					},
				)
				if (e.isReportable()) {
					snackbar.setAction(R.string.details) {
						MangaErrorDialog.show(supportFragmentManager, manga, e)
					}
				}
				snackbar.show()
			}
		}
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		binding.root.updatePadding(
			left = insets.left,
			right = insets.right,
		)
		if (insets.bottom > 0) {
			window.setNavigationBarTransparentCompat(this, binding.layoutBottom?.elevation ?: 0f, 0.9f)
		}
	}

	private fun onHistoryChanged(info: HistoryInfo) {
		with(binding.buttonRead) {
			if (info.history != null) {
				setText(R.string._continue)
				setIconResource(if (info.isIncognitoMode) R.drawable.ic_incognito else R.drawable.ic_play)
			} else {
				setText(R.string.read)
				setIconResource(if (info.isIncognitoMode) R.drawable.ic_incognito else R.drawable.ic_play)
			}
		}
		val text = when {
			!info.isValid -> getString(R.string.loading_)
			info.currentChapter >= 0 -> getString(R.string.chapter_d_of_d, info.currentChapter + 1, info.totalChapters)
			info.totalChapters == 0 -> getString(R.string.no_chapters)
			else -> resources.getQuantityString(R.plurals.chapters, info.totalChapters, info.totalChapters)
		}
		binding.headerChapters?.title = text
		binding.textViewTitle?.text = text
	}

	private fun onNewChaptersChanged(newChapters: Int) {
		viewBadge.counter = newChapters
	}

	fun showChapterMissingDialog(chapterId: Long) {
		val remoteManga = viewModel.getRemoteManga()
		if (remoteManga == null) {
			val snackbar = makeSnackbar(getString(R.string.chapter_is_missing), Snackbar.LENGTH_SHORT)
			snackbar.show()
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
						state = ReaderState(chapterId, 0, 0),
					),
				)
			}
			setNeutralButton(R.string.download) { _, _ ->
				DownloadService.start(this@DetailsActivity, remoteManga, setOf(chapterId))
			}
			setCancelable(true)
		}.show()
	}

	private fun showBranchPopupMenu() {
		val menu = PopupMenu(this, binding.headerChapters ?: binding.buttonDropdown)
		val currentBranch = viewModel.selectedBranchValue
		for (branch in viewModel.branches.value ?: return) {
			val item = menu.menu.add(R.id.group_branches, Menu.NONE, Menu.NONE, branch)
			item.isChecked = branch == currentBranch
		}
		menu.menu.setGroupCheckable(R.id.group_branches, true, true)
		menu.setOnMenuItemClickListener { item ->
			viewModel.setSelectedBranch(item.title?.toString())
			true
		}
		menu.show()
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

	private fun isTabletLayout() = binding.layoutBottom == null

	private fun showBottomSheet(isVisible: Boolean) {
		val view = binding.layoutBottom ?: return
		if (view.isVisible == isVisible) return
		val transition = Slide(Gravity.BOTTOM)
		transition.addTarget(view)
		transition.interpolator = AccelerateDecelerateInterpolator()
		TransitionManager.beginDelayedTransition(binding.root as ViewGroup, transition)
		view.isVisible = isVisible
	}

	private fun makeSnackbar(text: CharSequence, @BaseTransientBottomBar.Duration duration: Int): Snackbar {
		val sb = Snackbar.make(binding.containerDetails, text, duration)
		if (binding.layoutBottom?.isVisible == true) {
			sb.anchorView = binding.headerChapters
		}
		return sb
	}

	private class PrefetchObserver(
		private val context: Context,
	) : Observer<List<ChapterListItem>> {

		private var isCalled = false

		override fun onChanged(t: List<ChapterListItem>?) {
			if (t.isNullOrEmpty()) {
				return
			}
			if (!isCalled) {
				isCalled = true
				val item = t.find { it.hasFlag(ChapterListItem.FLAG_CURRENT) } ?: t.first()
				MangaPrefetchService.prefetchPages(context, item.chapter)
			}
		}
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
