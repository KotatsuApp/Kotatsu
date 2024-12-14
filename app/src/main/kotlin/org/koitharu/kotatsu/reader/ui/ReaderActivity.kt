package org.koitharu.kotatsu.reader.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.transition.Fade
import android.transition.Slide
import android.transition.TransitionManager
import android.transition.TransitionSet
import android.view.Gravity
import android.view.KeyEvent
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
import kotlinx.coroutines.flow.Flow
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
import org.koitharu.kotatsu.core.ui.util.MenuInvalidator
import org.koitharu.kotatsu.core.ui.widgets.ZoomControl
import org.koitharu.kotatsu.core.util.IdlingDetector
import org.koitharu.kotatsu.core.util.ext.hasGlobalPoint
import org.koitharu.kotatsu.core.util.ext.isAnimationsEnabled
import org.koitharu.kotatsu.core.util.ext.isRtl
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.observeEvent
import org.koitharu.kotatsu.core.util.ext.postDelayed
import org.koitharu.kotatsu.core.util.ext.setValueRounded
import org.koitharu.kotatsu.core.util.ext.zipWithPrevious
import org.koitharu.kotatsu.databinding.ActivityReaderBinding
import org.koitharu.kotatsu.details.ui.DetailsActivity
import org.koitharu.kotatsu.details.ui.pager.pages.PagesSavedObserver
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.reader.data.TapGridSettings
import org.koitharu.kotatsu.reader.domain.TapGridArea
import org.koitharu.kotatsu.reader.ui.config.ReaderConfigSheet
import org.koitharu.kotatsu.reader.ui.pager.ReaderPage
import org.koitharu.kotatsu.reader.ui.pager.ReaderUiState
import org.koitharu.kotatsu.reader.ui.tapgrid.TapGridDispatcher
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class ReaderActivity :
	BaseFullscreenActivity<ActivityReaderBinding>(),
	TapGridDispatcher.OnGridTouchListener,
	ReaderConfigSheet.Callback,
	ReaderControlDelegate.OnInteractionListener,
	OnApplyWindowInsetsListener,
	ReaderNavigationCallback,
	IdlingDetector.Callback,
	ZoomControl.ZoomControlListener {

	@Inject
	lateinit var settings: AppSettings

	@Inject
	lateinit var tapGridSettings: TapGridSettings

	@Inject
	lateinit var pageSaveHelperFactory: PageSaveHelper.Factory

	@Inject
	lateinit var scrollTimerFactory: ScrollTimer.Factory

	@Inject
	lateinit var screenOrientationHelper: ScreenOrientationHelper

	private val idlingDetector = IdlingDetector(TimeUnit.SECONDS.toMillis(10), this)

	private val viewModel: ReaderViewModel by viewModels()

	override val readerMode: ReaderMode?
		get() = readerManager.currentMode

	override var isAutoScrollEnabled: Boolean
		get() = scrollTimer.isEnabled
		set(value) {
			scrollTimer.isEnabled = value
		}

	private lateinit var scrollTimer: ScrollTimer
	private lateinit var pageSaveHelper: PageSaveHelper
	private lateinit var touchHelper: TapGridDispatcher
	private lateinit var controlDelegate: ReaderControlDelegate
	private var gestureInsets: Insets = Insets.NONE
	private lateinit var readerManager: ReaderManager
	private val hideUiRunnable = Runnable { setUiIsVisible(false) }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityReaderBinding.inflate(layoutInflater))
		screenOrientationHelper.init(settings.readerScreenOrientation)
		readerManager = ReaderManager(supportFragmentManager, viewBinding.container, settings)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		touchHelper = TapGridDispatcher(this, this)
		scrollTimer = scrollTimerFactory.create(this, this)
		pageSaveHelper = pageSaveHelperFactory.create(this)
		controlDelegate = ReaderControlDelegate(resources, settings, tapGridSettings, this)
		viewBinding.slider.setLabelFormatter(PageLabelFormatter())
		viewBinding.zoomControl.listener = this
		ReaderSliderListener(viewModel, this).attachToSlider(viewBinding.slider)
		insetsDelegate.interceptingWindowInsetsListener = this
		idlingDetector.bindToLifecycle(this)
		viewBinding.buttonPrev.setOnClickListener(controlDelegate)
		viewBinding.buttonNext.setOnClickListener(controlDelegate)

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
						dispatchNavigateUp()
					}
				},
			),
		)
		viewModel.readerMode.observe(this, Lifecycle.State.STARTED, this::onInitReader)
		viewModel.onPageSaved.observeEvent(this, PagesSavedObserver(viewBinding.container))
		viewModel.uiState.zipWithPrevious().observe(this, this::onUiStateChanged)
		viewModel.isLoading.observe(this, this::onLoadingStateChanged)
		viewModel.content.observe(this) {
			onLoadingStateChanged(viewModel.isLoading.value)
		}
		viewModel.isSliderVisible.observe(this) { updateSliderVisibility() }
		viewModel.isKeepScreenOnEnabled.observe(this, this::setKeepScreenOn)
		viewModel.isInfoBarEnabled.observe(this, ::onReaderBarChanged)
		val bottomMenuInvalidator = MenuInvalidator(viewBinding.toolbarBottom)
		viewModel.isBookmarkAdded.observe(this, bottomMenuInvalidator)
		viewModel.isPagesSheetEnabled.observe(this, bottomMenuInvalidator)
		viewModel.onShowToast.observeEvent(this) { msgId ->
			Snackbar.make(viewBinding.container, msgId, Snackbar.LENGTH_SHORT)
				.setAnchorView(viewBinding.appbarBottom)
				.show()
		}
		viewModel.isZoomControlsEnabled.observe(this) {
			viewBinding.zoomControl.isVisible = it
		}
		viewBinding.toolbarBottom.addMenuProvider(ReaderMenuProvider(this, readerManager, viewModel))
	}

	override fun getParentActivityIntent(): Intent? {
		val manga = viewModel.getMangaOrNull() ?: return null
		return DetailsActivity.newIntent(this, manga)
	}

	override fun onUserInteraction() {
		super.onUserInteraction()
		scrollTimer.onUserInteraction()
		idlingDetector.onUserInteraction()
	}

	override fun onPause() {
		super.onPause()
		viewModel.onPause()
	}

	override fun isNsfwContent(): Flow<Boolean> = viewModel.isMangaNsfw

	override fun onIdle() {
		viewModel.saveCurrentState(readerManager.currentReader?.getCurrentState())
	}

	override fun onZoomIn() {
		readerManager.currentReader?.onZoomIn()
	}

	override fun onZoomOut() {
		readerManager.currentReader?.onZoomOut()
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

	private fun onLoadingStateChanged(isLoading: Boolean) {
		val hasPages = viewModel.content.value.pages.isNotEmpty()
		viewBinding.layoutLoading.isVisible = isLoading && !hasPages
		if (isLoading && hasPages) {
			viewBinding.toastView.show(R.string.loading_)
		} else {
			viewBinding.toastView.hide()
		}
		viewBinding.toolbarBottom.invalidateMenu()
		invalidateOptionsMenu()
	}

	override fun onGridTouch(area: TapGridArea): Boolean {
		return isReaderResumed() && controlDelegate.onGridTouch(area)
	}

	override fun onGridLongTouch(area: TapGridArea) {
		if (isReaderResumed()) {
			controlDelegate.onGridLongTouch(area)
		}
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
			touchables?.none { it.hasGlobalPoint(rawX, rawY) } != false
		}
	}

	override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
		touchHelper.dispatchTouchEvent(ev)
		scrollTimer.onTouchEvent(ev)
		return super.dispatchTouchEvent(ev)
	}

	override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
		return controlDelegate.onKeyDown(keyCode) || super.onKeyDown(keyCode, event)
	}

	override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
		return controlDelegate.onKeyUp(keyCode, event) || super.onKeyUp(keyCode, event)
	}

	override fun onChapterSelected(chapter: MangaChapter): Boolean {
		viewModel.switchChapter(chapter.id, 0)
		return true
	}

	override fun onPageSelected(page: ReaderPage): Boolean {
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
		return true
	}

	override fun onReaderModeChanged(mode: ReaderMode) {
		viewModel.saveCurrentState(readerManager.currentReader?.getCurrentState())
		viewModel.switchMode(mode)
	}

	override fun onDoubleModeChanged(isEnabled: Boolean) {
		readerManager.setDoubleReaderMode(isEnabled)
	}

	private fun setKeepScreenOn(isKeep: Boolean) {
		if (isKeep) {
			window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
		} else {
			window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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
					transition.addTransition(Slide(Gravity.BOTTOM).addTarget(viewBinding.floatingToolbar))
				} ?: run {
					transition.addTransition(Slide(Gravity.END).addTarget(viewBinding.floatingToolbar))
				}
				TransitionManager.beginDelayedTransition(viewBinding.root, transition)
			}
			val isFullscreen = settings.isReaderFullscreenEnabled
			viewBinding.appbarTop.isVisible = isUiVisible
			viewBinding.appbarBottom?.isVisible = isUiVisible
			viewBinding.infoBar.isGone = isUiVisible || (!viewModel.isInfoBarEnabled.value)
			viewBinding.infoBar.isTimeVisible = isFullscreen
			updateSliderVisibility()
			systemUiController.setSystemUiVisible(isUiVisible || !isFullscreen)
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
		viewBinding.infoBar.updatePadding(
			top = systemBars.top,
		)
		return WindowInsetsCompat.Builder(insets)
			.setInsets(WindowInsetsCompat.Type.systemBars(), Insets.NONE)
			.build()
	}

	override fun onWindowInsetsChanged(insets: Insets) = Unit

	override fun switchPageBy(delta: Int) {
		readerManager.currentReader?.switchPageBy(delta)
	}

	override fun switchChapterBy(delta: Int) {
		viewModel.switchChapterBy(delta)
	}

	override fun openMenu() {
		viewModel.saveCurrentState(readerManager.currentReader?.getCurrentState())
		val currentMode = readerManager.currentMode ?: return
		ReaderConfigSheet.show(supportFragmentManager, currentMode)
	}

	override fun scrollBy(delta: Int, smooth: Boolean): Boolean {
		return readerManager.currentReader?.scrollBy(delta, smooth) == true
	}

	override fun toggleUiVisibility() {
		setUiIsVisible(!viewBinding.appbarTop.isVisible)
	}

	override fun isReaderResumed(): Boolean {
		val reader = readerManager.currentReader ?: return false
		return reader.isResumed && supportFragmentManager.fragments.lastOrNull() === reader
	}

	override fun onSavePageClick() {
		viewModel.saveCurrentPage(pageSaveHelper)
	}

	private fun onReaderBarChanged(isBarEnabled: Boolean) {
		viewBinding.infoBar.isVisible = isBarEnabled && viewBinding.appbarTop.isGone
	}

	private fun onUiStateChanged(pair: Pair<ReaderUiState?, ReaderUiState?>) {
		val (previous: ReaderUiState?, uiState: ReaderUiState?) = pair
		title = uiState?.mangaName ?: getString(R.string.loading_)
		viewBinding.infoBar.update(uiState)
		if (uiState == null) {
			supportActionBar?.subtitle = null
			updateSliderVisibility()
			return
		}
		supportActionBar?.subtitle = when {
			uiState.incognito -> getString(R.string.incognito_mode)
			else -> uiState.chapterName
		}
		if (previous?.chapterName != null && uiState.chapterName != previous.chapterName) {
			if (!uiState.chapterName.isNullOrEmpty()) {
				viewBinding.toastView.showTemporary(uiState.chapterName, TOAST_DURATION)
			}
		}
		if (uiState.isSliderAvailable()) {
			viewBinding.slider.valueTo = uiState.totalPages.toFloat() - 1
			viewBinding.slider.setValueRounded(uiState.currentPage.toFloat())
		}
		updateSliderVisibility()
	}

	private fun updateSliderVisibility() {
		viewBinding.floatingToolbar.isVisible = viewBinding.appbarTop.isVisible &&
			viewModel.isSliderVisible.value &&
			viewModel.uiState.value?.isSliderAvailable() == true
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
