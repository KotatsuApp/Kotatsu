package org.koitharu.kotatsu.reader.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.transition.Fade
import android.transition.Slide
import android.transition.TransitionManager
import android.transition.TransitionSet
import android.view.Gravity
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.core.graphics.Insets
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.bookmarks.domain.Bookmark
import org.koitharu.kotatsu.core.exceptions.resolve.DialogErrorObserver
import org.koitharu.kotatsu.core.model.parcelable.ParcelableManga
import org.koitharu.kotatsu.core.parser.MangaIntent
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ReaderMode
import org.koitharu.kotatsu.core.ui.BaseFullscreenActivity
import org.koitharu.kotatsu.core.util.GridTouchHelper
import org.koitharu.kotatsu.core.util.IdlingDetector
import org.koitharu.kotatsu.core.util.ShareHelper
import org.koitharu.kotatsu.core.util.ext.hasGlobalPoint
import org.koitharu.kotatsu.core.util.ext.isAnimationsEnabled
import org.koitharu.kotatsu.core.util.ext.isRtl
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.observeEvent
import org.koitharu.kotatsu.core.util.ext.postDelayed
import org.koitharu.kotatsu.core.util.ext.setValueRounded
import org.koitharu.kotatsu.core.util.ext.zipWithPrevious
import org.koitharu.kotatsu.databinding.ActivityReaderBinding
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.reader.ui.config.ReaderConfigSheet
import org.koitharu.kotatsu.reader.ui.pager.ReaderPage
import org.koitharu.kotatsu.reader.ui.pager.ReaderUiState
import org.koitharu.kotatsu.reader.ui.thumbnails.OnPageSelectListener
import org.koitharu.kotatsu.reader.ui.thumbnails.PagesThumbnailsSheet
import org.koitharu.kotatsu.settings.SettingsActivity
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class ReaderActivity :
	BaseFullscreenActivity<ActivityReaderBinding>(),
	ChaptersSheet.OnChapterChangeListener,
	GridTouchHelper.OnGridTouchListener,
	OnPageSelectListener,
	ReaderConfigSheet.Callback,
	ReaderControlDelegate.OnInteractionListener,
	OnApplyWindowInsetsListener,
	IdlingDetector.Callback {

	@Inject
	lateinit var settings: AppSettings

	private val idlingDetector = IdlingDetector(TimeUnit.SECONDS.toMillis(10), this)

	private val viewModel: ReaderViewModel by viewModels()

	override val readerMode: ReaderMode?
		get() = readerManager.currentMode

	override var isAutoScrollEnabled: Boolean
		get() = scrollTimer.isEnabled
		set(value) {
			scrollTimer.isEnabled = value
		}

	@Inject
	lateinit var scrollTimerFactory: ScrollTimer.Factory

	private lateinit var scrollTimer: ScrollTimer
	private lateinit var touchHelper: GridTouchHelper
	private lateinit var controlDelegate: ReaderControlDelegate
	private var gestureInsets: Insets = Insets.NONE
	private lateinit var readerManager: ReaderManager
	private val hideUiRunnable = Runnable { setUiIsVisible(false) }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityReaderBinding.inflate(layoutInflater))
		readerManager = ReaderManager(supportFragmentManager, R.id.container)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		touchHelper = GridTouchHelper(this, this)
		scrollTimer = scrollTimerFactory.create(this, this)
		controlDelegate = ReaderControlDelegate(resources, settings, this, this)
		viewBinding.toolbarBottom.setOnMenuItemClickListener(::onOptionsItemSelected)
		viewBinding.slider.setLabelFormatter(PageLabelFormatter())
		ReaderSliderListener(this, viewModel).attachToSlider(viewBinding.slider)
		insetsDelegate.interceptingWindowInsetsListener = this
		idlingDetector.bindToLifecycle(this)

		viewModel.onError.observeEvent(
			this,
			DialogErrorObserver(
				host = viewBinding.container,
				fragment = null,
				resolver = exceptionResolver,
				onResolved = { isResolved ->
					if (isResolved) {
						viewModel.reload()
					} else if (viewModel.content.value.pages.isEmpty()) {
						finishAfterTransition()
					}
				},
			),
		)
		viewModel.readerMode.observe(this, Lifecycle.State.STARTED, this::onInitReader)
		viewModel.onPageSaved.observeEvent(this, this::onPageSaved)
		viewModel.uiState.zipWithPrevious().observe(this, this::onUiStateChanged)
		viewModel.isLoading.observe(this, this::onLoadingStateChanged)
		viewModel.content.observe(this) {
			onLoadingStateChanged(viewModel.isLoading.value)
		}
		viewModel.isScreenshotsBlockEnabled.observe(this, this::setWindowSecure)
		viewModel.isInfoBarEnabled.observe(this, ::onReaderBarChanged)
		viewModel.isBookmarkAdded.observe(this, this::onBookmarkStateChanged)
		viewModel.onShowToast.observeEvent(this) { msgId ->
			Snackbar.make(viewBinding.container, msgId, Snackbar.LENGTH_SHORT)
				.setAnchorView(viewBinding.appbarBottom)
				.show()
		}
	}

	override fun onUserInteraction() {
		super.onUserInteraction()
		scrollTimer.onUserInteraction()
		idlingDetector.onUserInteraction()
	}

	override fun onIdle() {
		viewModel.saveCurrentState(readerManager.currentReader?.getCurrentState())
	}

	private fun onInitReader(mode: ReaderMode?) {
		if (mode == null) {
			return
		}
		if (readerManager.currentMode != mode) {
			readerManager.replace(mode)
		}
		if (viewBinding.appbarTop.isVisible) {
			lifecycle.postDelayed(TimeUnit.SECONDS.toMillis(1), hideUiRunnable)
		}
		viewBinding.slider.isRtl = mode == ReaderMode.REVERSED
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		menuInflater.inflate(R.menu.opt_reader_top, menu)
		return super.onCreateOptionsMenu(menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when (item.itemId) {
			R.id.action_settings -> {
				startActivity(SettingsActivity.newReaderSettingsIntent(this))
			}

			R.id.action_chapters -> {
				ChaptersSheet.show(supportFragmentManager)
			}

			R.id.action_pages_thumbs -> {
				val state = viewModel.getCurrentState() ?: return false
				PagesThumbnailsSheet.show(
					supportFragmentManager,
					viewModel.manga?.any ?: return false,
					state.chapterId,
					state.page,
				)
			}

			R.id.action_bookmark -> {
				if (viewModel.isBookmarkAdded.value) {
					viewModel.removeBookmark()
				} else {
					viewModel.addBookmark()
				}
			}

			R.id.action_options -> {
				viewModel.saveCurrentState(readerManager.currentReader?.getCurrentState())
				val currentMode = readerManager.currentMode ?: return false
				ReaderConfigSheet.show(supportFragmentManager, currentMode)
			}

			else -> return super.onOptionsItemSelected(item)
		}
		return true
	}

	private fun onLoadingStateChanged(isLoading: Boolean) {
		val hasPages = viewModel.content.value.pages.isNotEmpty()
		viewBinding.layoutLoading.isVisible = isLoading && !hasPages
		if (isLoading && hasPages) {
			viewBinding.toastView.show(R.string.loading_)
		} else {
			viewBinding.toastView.hide()
		}
		val menu = viewBinding.toolbarBottom.menu
		menu.findItem(R.id.action_bookmark).isVisible = hasPages
		menu.findItem(R.id.action_pages_thumbs).isVisible = hasPages
	}

	override fun onGridTouch(area: Int) {
		controlDelegate.onGridTouch(area, viewBinding.container)
	}

	override fun onProcessTouch(rawX: Int, rawY: Int): Boolean {
		return if (
			rawX <= gestureInsets.left ||
			rawY <= gestureInsets.top ||
			rawX >= viewBinding.root.width - gestureInsets.right ||
			rawY >= viewBinding.root.height - gestureInsets.bottom ||
			viewBinding.appbarTop.hasGlobalPoint(rawX, rawY) ||
			viewBinding.appbarBottom?.hasGlobalPoint(rawX, rawY) == true
		) {
			false
		} else {
			val touchables = window.peekDecorView()?.touchables
			touchables?.none { it.hasGlobalPoint(rawX, rawY) } ?: true
		}
	}

	override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
		touchHelper.dispatchTouchEvent(ev)
		return super.dispatchTouchEvent(ev)
	}

	override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
		return controlDelegate.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event)
	}

	override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
		return controlDelegate.onKeyUp(keyCode, event) || super.onKeyUp(keyCode, event)
	}

	override fun onChapterChanged(chapter: MangaChapter) {
		viewModel.switchChapter(chapter.id, 0)
	}

	override fun onPageSelected(page: ReaderPage) {
		lifecycleScope.launch(Dispatchers.Default) {
			val pages = viewModel.content.value.pages
			val index = pages.indexOfFirst { it.chapterId == page.chapterId && it.id == page.id }
			if (index != -1) {
				withContext(Dispatchers.Main) {
					readerManager.currentReader?.switchPageTo(index, true)
				}
			} else {
				viewModel.switchChapter(page.chapterId, page.index)
			}
		}
	}

	override fun onReaderModeChanged(mode: ReaderMode) {
		viewModel.saveCurrentState(readerManager.currentReader?.getCurrentState())
		viewModel.switchMode(mode)
	}

	private fun onPageSaved(uri: Uri?) {
		if (uri != null) {
			Snackbar.make(viewBinding.container, R.string.page_saved, Snackbar.LENGTH_LONG)
				.setAnchorView(viewBinding.appbarBottom)
				.setAction(R.string.share) {
					ShareHelper(this).shareImage(uri)
				}.show()
		} else {
			Snackbar.make(viewBinding.container, R.string.error_occurred, Snackbar.LENGTH_SHORT)
				.setAnchorView(viewBinding.appbarBottom)
				.show()
		}
	}

	private fun setWindowSecure(isSecure: Boolean) {
		if (isSecure) {
			window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
		} else {
			window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
		}
	}

	private fun setUiIsVisible(isUiVisible: Boolean) {
		if (viewBinding.appbarTop.isVisible != isUiVisible) {
			if (isAnimationsEnabled) {
				val transition = TransitionSet()
					.setOrdering(TransitionSet.ORDERING_TOGETHER)
					.addTransition(Slide(Gravity.TOP).addTarget(viewBinding.appbarTop))
					.addTransition(Fade().addTarget(viewBinding.infoBar))
				viewBinding.appbarBottom?.let { bottomBar ->
					transition.addTransition(Slide(Gravity.BOTTOM).addTarget(bottomBar))
				}
				TransitionManager.beginDelayedTransition(viewBinding.root, transition)
			}
			viewBinding.appbarTop.isVisible = isUiVisible
			viewBinding.appbarBottom?.isVisible = isUiVisible
			viewBinding.infoBar.isGone = isUiVisible || (!viewModel.isInfoBarEnabled.value)
			systemUiController.setSystemUiVisible(isUiVisible)
		}
	}

	override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
		gestureInsets = insets.getInsets(WindowInsetsCompat.Type.systemGestures())
		val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
		viewBinding.appbarTop.updatePadding(
			top = systemBars.top,
			right = systemBars.right,
			left = systemBars.left,
		)
		viewBinding.appbarBottom?.updateLayoutParams<MarginLayoutParams> {
			bottomMargin = systemBars.bottom + topMargin
			rightMargin = systemBars.right + topMargin
			leftMargin = systemBars.left + topMargin
		}
		return WindowInsetsCompat.Builder(insets)
			.setInsets(WindowInsetsCompat.Type.systemBars(), Insets.NONE)
			.build()
	}

	override fun onWindowInsetsChanged(insets: Insets) = Unit

	override fun switchPageBy(delta: Int) {
		readerManager.currentReader?.switchPageBy(delta)
	}

	override fun scrollBy(delta: Int, smooth: Boolean): Boolean {
		return readerManager.currentReader?.scrollBy(delta, smooth) ?: false
	}

	override fun toggleUiVisibility() {
		setUiIsVisible(!viewBinding.appbarTop.isVisible)
	}

	override fun isReaderResumed(): Boolean {
		val reader = readerManager.currentReader ?: return false
		return reader.isResumed && supportFragmentManager.fragments.lastOrNull() === reader
	}

	private fun onReaderBarChanged(isBarEnabled: Boolean) {
		viewBinding.infoBar.isVisible = isBarEnabled && viewBinding.appbarTop.isGone
	}

	private fun onBookmarkStateChanged(isAdded: Boolean) {
		val menuItem = viewBinding.toolbarBottom.menu.findItem(R.id.action_bookmark) ?: return
		menuItem.setTitle(if (isAdded) R.string.bookmark_remove else R.string.bookmark_add)
		menuItem.setIcon(if (isAdded) R.drawable.ic_bookmark_added else R.drawable.ic_bookmark)
	}

	private fun onUiStateChanged(pair: Pair<ReaderUiState?, ReaderUiState?>) {
		val (previous: ReaderUiState?, uiState: ReaderUiState?) = pair
		title = uiState?.resolveTitle(this) ?: getString(R.string.loading_)
		viewBinding.infoBar.update(uiState)
		if (uiState == null) {
			supportActionBar?.subtitle = null
			viewBinding.slider.isVisible = false
			return
		}
		supportActionBar?.subtitle = uiState.chapterName
		if (previous?.chapterName != null && uiState.chapterName != previous.chapterName) {
			if (!uiState.chapterName.isNullOrEmpty()) {
				viewBinding.toastView.showTemporary(uiState.chapterName, TOAST_DURATION)
			}
		}
		if (uiState.isSliderAvailable()) {
			viewBinding.slider.valueTo = uiState.totalPages.toFloat() - 1
			viewBinding.slider.setValueRounded(uiState.currentPage.toFloat())
			viewBinding.slider.isVisible = true
		} else {
			viewBinding.slider.isVisible = false
		}
	}

	class IntentBuilder(context: Context) {

		private val intent = Intent(context, ReaderActivity::class.java)
			.setAction(ACTION_MANGA_READ)

		fun manga(manga: Manga) = apply {
			intent.putExtra(MangaIntent.KEY_MANGA, ParcelableManga(manga))
		}

		fun mangaId(mangaId: Long) = apply {
			intent.putExtra(MangaIntent.KEY_ID, mangaId)
		}

		fun incognito(incognito: Boolean) = apply {
			intent.putExtra(EXTRA_INCOGNITO, incognito)
		}

		fun branch(branch: String?) = apply {
			intent.putExtra(EXTRA_BRANCH, branch)
		}

		fun state(state: ReaderState?) = apply {
			intent.putExtra(EXTRA_STATE, state)
		}

		fun bookmark(bookmark: Bookmark) = manga(
			bookmark.manga,
		).state(
			ReaderState(
				chapterId = bookmark.chapterId,
				page = bookmark.page,
				scroll = bookmark.scroll,
			),
		)

		fun build() = intent
	}

	companion object {

		const val ACTION_MANGA_READ = "${BuildConfig.APPLICATION_ID}.action.READ_MANGA"
		const val EXTRA_STATE = "state"
		const val EXTRA_BRANCH = "branch"
		const val EXTRA_INCOGNITO = "incognito"
		private const val TOAST_DURATION = 1500L
	}
}
