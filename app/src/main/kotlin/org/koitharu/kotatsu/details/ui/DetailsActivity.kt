package org.koitharu.kotatsu.details.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.transition.AutoTransition
import android.transition.Slide
import android.transition.TransitionManager
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.widget.PopupMenu
import androidx.core.graphics.Insets
import androidx.core.text.buildSpannedString
import androidx.core.text.inSpans
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.filterNotNull
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.exceptions.resolve.SnackbarErrorObserver
import org.koitharu.kotatsu.core.model.parcelable.ParcelableManga
import org.koitharu.kotatsu.core.os.AppShortcutManager
import org.koitharu.kotatsu.core.parser.MangaIntent
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.core.ui.util.MenuInvalidator
import org.koitharu.kotatsu.core.util.ViewBadge
import org.koitharu.kotatsu.core.util.ext.doOnExpansionsChanged
import org.koitharu.kotatsu.core.util.ext.getAnimationDuration
import org.koitharu.kotatsu.core.util.ext.getThemeColor
import org.koitharu.kotatsu.core.util.ext.isAnimationsEnabled
import org.koitharu.kotatsu.core.util.ext.measureHeight
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.observeEvent
import org.koitharu.kotatsu.core.util.ext.setNavigationBarTransparentCompat
import org.koitharu.kotatsu.core.util.ext.setNavigationIconSafe
import org.koitharu.kotatsu.core.util.ext.setOnContextClickListenerCompat
import org.koitharu.kotatsu.core.util.ext.textAndVisible
import org.koitharu.kotatsu.databinding.ActivityDetailsBinding
import org.koitharu.kotatsu.details.service.MangaPrefetchService
import org.koitharu.kotatsu.details.ui.model.ChapterListItem
import org.koitharu.kotatsu.details.ui.model.HistoryInfo
import org.koitharu.kotatsu.download.ui.worker.DownloadStartedObserver
import org.koitharu.kotatsu.main.ui.owners.NoModalBottomSheetOwner
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.reader.ui.ReaderActivity.IntentBuilder
import org.koitharu.kotatsu.reader.ui.thumbnails.PagesThumbnailsSheet
import java.lang.ref.WeakReference
import javax.inject.Inject
import com.google.android.material.R as materialR

@AndroidEntryPoint
class DetailsActivity :
	BaseActivity<ActivityDetailsBinding>(),
	View.OnClickListener,
	NoModalBottomSheetOwner,
	View.OnLongClickListener,
	PopupMenu.OnMenuItemClickListener {

	@Inject
	lateinit var appShortcutManager: AppShortcutManager

	private lateinit var viewBadge: ViewBadge
	private var buttonTip: WeakReference<ButtonTip>? = null

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
		viewBinding.buttonRead.setOnContextClickListenerCompat(this)
		viewBinding.buttonDropdown.setOnClickListener(this)
		viewBadge = ViewBadge(viewBinding.buttonRead, this)

		if (viewBinding.layoutBottom != null) {
			val behavior = BottomSheetBehavior.from(checkNotNull(viewBinding.layoutBottom))
			val bsMediator = ChaptersBottomSheetMediator(behavior)
			actionModeDelegate.addListener(bsMediator)
			checkNotNull(viewBinding.layoutBsHeader).addOnLayoutChangeListener(bsMediator)
			onBackPressedDispatcher.addCallback(bsMediator)
			chaptersMenuProvider = ChaptersMenuProvider(viewModel, bsMediator)
			behavior.doOnExpansionsChanged(::onChaptersSheetStateChanged)
			viewBinding.toolbarChapters?.setNavigationOnClickListener {
				behavior.state = BottomSheetBehavior.STATE_COLLAPSED
			}
			viewBinding.toolbarChapters?.setOnGenericMotionListener(bsMediator)
		} else {
			chaptersMenuProvider = ChaptersMenuProvider(viewModel, null)
			addMenuProvider(chaptersMenuProvider)
		}
		onBackPressedDispatcher.addCallback(chaptersMenuProvider)

		viewModel.manga.filterNotNull().observe(this, ::onMangaUpdated)
		viewModel.newChaptersCount.observe(this, ::onNewChaptersChanged)
		viewModel.onMangaRemoved.observeEvent(this, ::onMangaRemoved)
		viewModel.onError.observeEvent(
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
		viewModel.onShowToast.observeEvent(this) {
			makeSnackbar(getString(it), Snackbar.LENGTH_SHORT).show()
		}
		viewModel.onShowTip.observeEvent(this) { showTip() }
		viewModel.historyInfo.observe(this, ::onHistoryChanged)
		viewModel.selectedBranch.observe(this) {
			viewBinding.toolbarChapters?.subtitle = it
			viewBinding.textViewSubtitle?.textAndVisible = it
		}
		viewModel.isChaptersReversed.observe(
			this,
			MenuInvalidator(viewBinding.toolbarChapters ?: this)
		)
		viewModel.favouriteCategories.observe(this, MenuInvalidator(this))
		viewModel.branches.observe(this) {
			viewBinding.buttonDropdown.isVisible = it.size > 1
		}
		viewModel.chapters.observe(this, PrefetchObserver(this))
		viewModel.onDownloadStarted.observeEvent(
			this,
			DownloadStartedObserver(viewBinding.containerDetails)
		)

		addMenuProvider(
			DetailsMenuProvider(
				activity = this,
				viewModel = viewModel,
				snackbarHost = viewBinding.containerChapters,
				appShortcutManager = appShortcutManager,
			),
		)
	}

	override fun getBottomSheetCollapsedHeight(): Int {
		return viewBinding.layoutBsHeader?.measureHeight() ?: 0
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_read -> openReader(isIncognitoMode = false)
			R.id.button_dropdown -> showBranchPopupMenu(v)
		}
	}

	override fun onLongClick(v: View): Boolean = when (v.id) {
		R.id.button_read -> {
			buttonTip?.get()?.remove()
			buttonTip = null
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
				val history = viewModel.historyInfo.value.history
				PagesThumbnailsSheet.show(
					fm = supportFragmentManager,
					manga = viewModel.manga.value ?: return false,
					chapterId = history?.chapterId
						?: viewModel.chapters.value.firstOrNull()?.chapter?.id
						?: return false,
					currentPage = history?.page ?: 0,
				)
				true
			}

			else -> false
		}
	}

	private fun onChaptersSheetStateChanged(isExpanded: Boolean) {
		val toolbar = viewBinding.toolbarChapters ?: return
		if (isAnimationsEnabled) {
			val transition = AutoTransition()
			transition.duration = getAnimationDuration(R.integer.config_shorterAnimTime)
			TransitionManager.beginDelayedTransition(toolbar, transition)
		}
		if (isExpanded) {
			toolbar.addMenuProvider(chaptersMenuProvider)
			toolbar.setNavigationIconSafe(materialR.drawable.abc_ic_clear_material)
		} else {
			toolbar.removeMenuProvider(chaptersMenuProvider)
			toolbar.navigationIcon = null
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
			window.setNavigationBarTransparentCompat(
				this,
				viewBinding.layoutBottom?.elevation ?: 0f,
				0.9f
			)
		}
		viewBinding.cardChapters?.updateLayoutParams<MarginLayoutParams> {
			bottomMargin = insets.bottom + marginEnd
		}
		viewBinding.dragHandle?.updateLayoutParams<MarginLayoutParams> {
			bottomMargin = insets.top
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
			info.currentChapter >= 0 -> getString(
				R.string.chapter_d_of_d,
				info.currentChapter + 1,
				info.totalChapters
			)

			info.totalChapters == 0 -> getString(R.string.no_chapters)
			else -> resources.getQuantityString(
				R.plurals.chapters,
				info.totalChapters,
				info.totalChapters
			)
		}
		viewBinding.toolbarChapters?.title = text
		viewBinding.textViewTitle?.text = text
	}

	private fun onNewChaptersChanged(newChapters: Int) {
		viewBadge.counter = newChapters
	}

	private fun showBranchPopupMenu(v: View) {
		val menu = PopupMenu(v.context, v)
		val branches = viewModel.branches.value
		for ((i, branch) in branches.withIndex()) {
			val title = buildSpannedString {
				append(branch.name ?: getString(R.string.system_default))
				append(' ')
				append(' ')
				inSpans(
					ForegroundColorSpan(
						v.context.getThemeColor(
							android.R.attr.textColorSecondary,
							Color.LTGRAY
						)
					),
					RelativeSizeSpan(0.74f),
				) {
					append(branch.count.toString())
				}
			}
			menu.menu.add(Menu.NONE, Menu.NONE, i, title)
		}
		menu.setOnMenuItemClickListener {
			viewModel.setSelectedBranch(branches.getOrNull(it.order)?.name)
			true
		}
		menu.show()
	}

	private fun openReader(isIncognitoMode: Boolean) {
		val manga = viewModel.manga.value ?: return
		val chapterId = viewModel.historyInfo.value.history?.chapterId
		if (chapterId != null && manga.chapters?.none { x -> x.id == chapterId } == true) {
			val snackbar =
				makeSnackbar(getString(R.string.chapter_is_missing), Snackbar.LENGTH_SHORT)
			snackbar.show()
		} else {
			startActivity(
				IntentBuilder(this)
					.manga(manga)
					.branch(viewModel.selectedBranchValue)
					.incognito(isIncognitoMode)
					.build(),
			)
			if (isIncognitoMode) {
				Toast.makeText(this, R.string.incognito_mode, Toast.LENGTH_SHORT).show()
			}
		}
	}

	private fun showBottomSheet(isVisible: Boolean) {
		val view = viewBinding.layoutBottom ?: return
		if (view.isVisible == isVisible) return
		val transition = Slide(Gravity.BOTTOM)
		transition.addTarget(view)
		transition.interpolator = AccelerateDecelerateInterpolator()
		TransitionManager.beginDelayedTransition(viewBinding.root as ViewGroup, transition)
		view.isVisible = isVisible
	}

	private fun makeSnackbar(
		text: CharSequence,
		@BaseTransientBottomBar.Duration duration: Int,
	): Snackbar {
		val sb = Snackbar.make(viewBinding.containerDetails, text, duration)
		if (viewBinding.layoutBottom?.isVisible == true) {
			sb.anchorView = viewBinding.toolbarChapters
		}
		return sb
	}

	private class PrefetchObserver(
		private val context: Context,
	) : FlowCollector<List<ChapterListItem>?> {

		private var isCalled = false

		override suspend fun emit(value: List<ChapterListItem>?) {
			if (value.isNullOrEmpty()) {
				return
			}
			if (!isCalled) {
				isCalled = true
				val item = value.find { it.isCurrent } ?: value.first()
				MangaPrefetchService.prefetchPages(context, item.chapter)
			}
		}
	}

	private fun showTip() {
		val tip = ButtonTip(viewBinding.root as ViewGroup, insetsDelegate, viewModel)
		tip.addToRoot()
		buttonTip = WeakReference(tip)
	}

	companion object {

		const val TIP_BUTTON = "btn_read"

		fun newIntent(context: Context, manga: Manga): Intent {
			return Intent(context, DetailsActivity::class.java)
				.putExtra(MangaIntent.KEY_MANGA, ParcelableManga(manga))
		}

		fun newIntent(context: Context, mangaId: Long): Intent {
			return Intent(context, DetailsActivity::class.java)
				.putExtra(MangaIntent.KEY_ID, mangaId)
		}
	}
}
