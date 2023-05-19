package org.koitharu.kotatsu.details.ui

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.transition.Slide
import android.transition.TransitionManager
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.widget.PopupMenu
import androidx.core.graphics.Insets
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.Observer
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.exceptions.resolve.SnackbarErrorObserver
import org.koitharu.kotatsu.core.model.parcelable.ParcelableManga
import org.koitharu.kotatsu.core.os.ShortcutsUpdater
import org.koitharu.kotatsu.core.parser.MangaIntent
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.core.ui.dialog.RecyclerViewAlertDialog
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.ui.widgets.BottomSheetHeaderBar
import org.koitharu.kotatsu.core.util.ViewBadge
import org.koitharu.kotatsu.core.util.ext.setNavigationBarTransparentCompat
import org.koitharu.kotatsu.core.util.ext.textAndVisible
import org.koitharu.kotatsu.databinding.ActivityDetailsBinding
import org.koitharu.kotatsu.details.service.MangaPrefetchService
import org.koitharu.kotatsu.details.ui.adapter.branchAD
import org.koitharu.kotatsu.details.ui.model.ChapterListItem
import org.koitharu.kotatsu.details.ui.model.HistoryInfo
import org.koitharu.kotatsu.details.ui.model.MangaBranch
import org.koitharu.kotatsu.download.ui.worker.DownloadStartedObserver
import org.koitharu.kotatsu.main.ui.owners.NoModalBottomSheetOwner
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.reader.ui.ReaderActivity
import org.koitharu.kotatsu.reader.ui.ReaderState
import org.koitharu.kotatsu.reader.ui.thumbnails.PagesThumbnailsSheet
import javax.inject.Inject

@AndroidEntryPoint
class DetailsActivity :
	BaseActivity<ActivityDetailsBinding>(),
	View.OnClickListener,
	BottomSheetHeaderBar.OnExpansionChangeListener,
	NoModalBottomSheetOwner,
	View.OnLongClickListener,
	PopupMenu.OnMenuItemClickListener {

	override val bsHeader: BottomSheetHeaderBar?
		get() = viewBinding.headerChapters

	@Inject
	lateinit var shortcutsUpdater: ShortcutsUpdater

	private lateinit var viewBadge: ViewBadge

	private val viewModel: DetailsViewModel by viewModels()
	private lateinit var chaptersMenuProvider: ChaptersMenuProvider

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityDetailsBinding.inflate(layoutInflater))
		supportActionBar?.run {
			setDisplayHomeAsUpEnabled(true)
			setDisplayShowTitleEnabled(false)
		}
		viewBinding.buttonRead.setOnClickListener(this)
		viewBinding.buttonRead.setOnLongClickListener(this)
		viewBinding.buttonDropdown.setOnClickListener(this)
		viewBadge = ViewBadge(viewBinding.buttonRead, this)

		chaptersMenuProvider = if (viewBinding.layoutBottom != null) {
			val bsMediator = ChaptersBottomSheetMediator(checkNotNull(viewBinding.layoutBottom))
			actionModeDelegate.addListener(bsMediator)
			checkNotNull(viewBinding.headerChapters).addOnExpansionChangeListener(bsMediator)
			checkNotNull(viewBinding.headerChapters).addOnLayoutChangeListener(bsMediator)
			onBackPressedDispatcher.addCallback(bsMediator)
			ChaptersMenuProvider(viewModel, bsMediator)
		} else {
			ChaptersMenuProvider(viewModel, null)
		}

		viewModel.manga.observe(this, ::onMangaUpdated)
		viewModel.newChaptersCount.observe(this, ::onNewChaptersChanged)
		viewModel.onMangaRemoved.observe(this, ::onMangaRemoved)
		viewModel.onError.observe(
			this,
			SnackbarErrorObserver(
				host = viewBinding.containerDetails,
				fragment = null,
				resolver = exceptionResolver,
				onResolved = { isResolved ->
					if (isResolved) {
						viewModel.reload()
					}
				},
			),
		)
		viewModel.onShowToast.observe(this) {
			makeSnackbar(getString(it), Snackbar.LENGTH_SHORT).show()
		}
		viewModel.historyInfo.observe(this, ::onHistoryChanged)
		viewModel.selectedBranchName.observe(this) {
			viewBinding.headerChapters?.subtitle = it
			viewBinding.textViewSubtitle?.textAndVisible = it
		}
		viewModel.isChaptersReversed.observe(this) {
			viewBinding.headerChapters?.invalidateMenu() ?: invalidateOptionsMenu()
		}
		viewModel.favouriteCategories.observe(this) {
			invalidateOptionsMenu()
		}
		viewModel.branches.observe(this) {
			viewBinding.buttonDropdown.isVisible = it.size > 1
		}
		viewModel.chapters.observe(this, PrefetchObserver(this))
		viewModel.onDownloadStarted.observe(this, DownloadStartedObserver(viewBinding.containerDetails))

		addMenuProvider(
			DetailsMenuProvider(
				activity = this,
				viewModel = viewModel,
				snackbarHost = viewBinding.containerChapters,
				shortcutsUpdater = shortcutsUpdater,
			),
		)
		viewBinding.headerChapters?.addOnExpansionChangeListener(this) ?: addMenuProvider(chaptersMenuProvider)
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_read -> openReader(isIncognitoMode = false)
			R.id.button_dropdown -> showBranchPopupMenu()
		}
	}

	override fun onLongClick(v: View): Boolean = when (v.id) {
		R.id.button_read -> {
			val menu = PopupMenu(v.context, v)
			menu.inflate(R.menu.popup_read)
			menu.setOnMenuItemClickListener(this)
			menu.setForceShowIcon(true)
			menu.show()
			true
		}

		else -> false
	}

	override fun onMenuItemClick(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.action_incognito -> {
				openReader(isIncognitoMode = true)
				true
			}

			R.id.action_pages_thumbs -> {
				val history = viewModel.historyInfo.value?.history
				PagesThumbnailsSheet.show(
					fm = supportFragmentManager,
					manga = viewModel.manga.value ?: return false,
					chapterId = history?.chapterId
						?: viewModel.chapters.value?.firstOrNull()?.chapter?.id
						?: return false,
					currentPage = history?.page ?: 0,
				)
				true
			}

			else -> false
		}
	}

	override fun onExpansionStateChanged(headerBar: BottomSheetHeaderBar, isExpanded: Boolean) {
		if (isExpanded) {
			headerBar.addMenuProvider(chaptersMenuProvider)
		} else {
			headerBar.removeMenuProvider(chaptersMenuProvider)
		}
		viewBinding.buttonRead.isGone = isExpanded
	}

	private fun onMangaUpdated(manga: Manga) {
		title = manga.title
		val hasChapters = !manga.chapters.isNullOrEmpty()
		viewBinding.buttonRead.isEnabled = hasChapters
		invalidateOptionsMenu()
		showBottomSheet(manga.chapters != null)
		viewBinding.groupHeader?.isVisible = hasChapters
	}

	private fun onMangaRemoved(manga: Manga) {
		Toast.makeText(
			this,
			getString(R.string._s_deleted_from_local_storage, manga.title),
			Toast.LENGTH_SHORT,
		).show()
		finishAfterTransition()
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		viewBinding.root.updatePadding(
			left = insets.left,
			right = insets.right,
		)
		if (insets.bottom > 0) {
			window.setNavigationBarTransparentCompat(this, viewBinding.layoutBottom?.elevation ?: 0f, 0.9f)
		}
	}

	private fun onHistoryChanged(info: HistoryInfo) {
		with(viewBinding.buttonRead) {
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
		viewBinding.headerChapters?.title = text
		viewBinding.textViewTitle?.text = text
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
				viewModel.download(setOf(chapterId))
			}
			setCancelable(true)
		}.show()
	}

	private fun showBranchPopupMenu() {
		var dialog: DialogInterface? = null
		val listener = OnListItemClickListener<MangaBranch> { item, _ ->
			viewModel.setSelectedBranch(item.name)
			dialog?.dismiss()
		}
		dialog = RecyclerViewAlertDialog.Builder<MangaBranch>(this)
			.addAdapterDelegate(branchAD(listener))
			.setCancelable(true)
			.setNegativeButton(android.R.string.cancel, null)
			.setTitle(R.string.translations)
			.setItems(viewModel.branches.value.orEmpty())
			.create()
			.also { it.show() }
	}

	private fun openReader(isIncognitoMode: Boolean) {
		val manga = viewModel.manga.value ?: return
		val chapterId = viewModel.historyInfo.value?.history?.chapterId
		if (chapterId != null && manga.chapters?.none { x -> x.id == chapterId } == true) {
			showChapterMissingDialog(chapterId)
		} else {
			startActivity(
				ReaderActivity.newIntent(
					context = this,
					manga = manga,
					branch = viewModel.selectedBranchValue,
					isIncognitoMode = isIncognitoMode,
				),
			)
			if (isIncognitoMode) {
				Toast.makeText(this, R.string.incognito_mode, Toast.LENGTH_SHORT).show()
			}
		}
	}

	private fun isTabletLayout() = viewBinding.layoutBottom == null

	private fun showBottomSheet(isVisible: Boolean) {
		val view = viewBinding.layoutBottom ?: return
		if (view.isVisible == isVisible) return
		val transition = Slide(Gravity.BOTTOM)
		transition.addTarget(view)
		transition.interpolator = AccelerateDecelerateInterpolator()
		TransitionManager.beginDelayedTransition(viewBinding.root as ViewGroup, transition)
		view.isVisible = isVisible
	}

	private fun makeSnackbar(text: CharSequence, @BaseTransientBottomBar.Duration duration: Int): Snackbar {
		val sb = Snackbar.make(viewBinding.containerDetails, text, duration)
		if (viewBinding.layoutBottom?.isVisible == true) {
			sb.anchorView = viewBinding.headerChapters
		}
		return sb
	}

	private class PrefetchObserver(
		private val context: Context,
	) : Observer<List<ChapterListItem>?> {

		private var isCalled = false

		override fun onChanged(value: List<ChapterListItem>?) {
			if (value.isNullOrEmpty()) {
				return
			}
			if (!isCalled) {
				isCalled = true
				val item = value.find { it.hasFlag(ChapterListItem.FLAG_CURRENT) } ?: value.first()
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
