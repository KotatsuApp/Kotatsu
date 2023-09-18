package org.koitharu.kotatsu.reader.ui

import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.bookmarks.domain.Bookmark
import org.koitharu.kotatsu.bookmarks.domain.BookmarksRepository
import org.koitharu.kotatsu.core.os.AppShortcutManager
import org.koitharu.kotatsu.core.parser.MangaDataRepository
import org.koitharu.kotatsu.core.parser.MangaIntent
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ReaderMode
import org.koitharu.kotatsu.core.prefs.ScreenshotsPolicy
import org.koitharu.kotatsu.core.prefs.observeAsFlow
import org.koitharu.kotatsu.core.prefs.observeAsStateFlow
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.util.ext.MutableEventFlow
import org.koitharu.kotatsu.core.util.ext.call
import org.koitharu.kotatsu.core.util.ext.ifNullOrEmpty
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import org.koitharu.kotatsu.core.util.ext.requireValue
import org.koitharu.kotatsu.details.domain.DoubleMangaLoadUseCase
import org.koitharu.kotatsu.details.domain.model.DoubleManga
import org.koitharu.kotatsu.history.data.HistoryRepository
import org.koitharu.kotatsu.history.data.PROGRESS_NONE
import org.koitharu.kotatsu.history.domain.HistoryUpdateUseCase
import org.koitharu.kotatsu.parsers.exception.NotFoundException
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.reader.domain.ChaptersLoader
import org.koitharu.kotatsu.reader.domain.DetectReaderModeUseCase
import org.koitharu.kotatsu.reader.domain.PageLoader
import org.koitharu.kotatsu.reader.ui.config.ReaderSettings
import org.koitharu.kotatsu.reader.ui.pager.ReaderUiState
import java.util.Date
import javax.inject.Inject

private const val BOUNDS_PAGE_OFFSET = 2
private const val PREFETCH_LIMIT = 10

@HiltViewModel
class ReaderViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	private val dataRepository: MangaDataRepository,
	private val historyRepository: HistoryRepository,
	private val bookmarksRepository: BookmarksRepository,
	private val settings: AppSettings,
	private val pageSaveHelper: PageSaveHelper,
	private val pageLoader: PageLoader,
	private val chaptersLoader: ChaptersLoader,
	private val appShortcutManager: AppShortcutManager,
	private val doubleMangaLoadUseCase: DoubleMangaLoadUseCase,
	private val historyUpdateUseCase: HistoryUpdateUseCase,
	private val detectReaderModeUseCase: DetectReaderModeUseCase,
) : BaseViewModel() {

	private val intent = MangaIntent(savedStateHandle)
	private val preselectedBranch = savedStateHandle.get<String>(ReaderActivity.EXTRA_BRANCH)
	private val isIncognito = savedStateHandle.get<Boolean>(ReaderActivity.EXTRA_INCOGNITO) ?: false

	private var loadingJob: Job? = null
	private var pageSaveJob: Job? = null
	private var bookmarkJob: Job? = null
	private var stateChangeJob: Job? = null
	private val currentState =
		MutableStateFlow<ReaderState?>(savedStateHandle[ReaderActivity.EXTRA_STATE])
	private val mangaData = MutableStateFlow(intent.manga?.let { DoubleManga(it) })
	private val mangaFlow: Flow<Manga?>
		get() = mangaData.map { it?.any }

	val readerMode = MutableStateFlow<ReaderMode?>(null)
	val onPageSaved = MutableEventFlow<Uri?>()
	val onShowToast = MutableEventFlow<Int>()
	val uiState = MutableStateFlow<ReaderUiState?>(null)

	val content = MutableStateFlow(ReaderContent(emptyList(), null))
	val manga: DoubleManga?
		get() = mangaData.value

	val pageAnimation = settings.observeAsStateFlow(
		scope = viewModelScope + Dispatchers.Default,
		key = AppSettings.KEY_READER_ANIMATION,
		valueProducer = { readerAnimation },
	)

	val isInfoBarEnabled = settings.observeAsStateFlow(
		scope = viewModelScope + Dispatchers.Default,
		key = AppSettings.KEY_READER_BAR,
		valueProducer = { isReaderBarEnabled },
	)

	val isWebtoonZoomEnabled = settings.observeAsStateFlow(
		scope = viewModelScope + Dispatchers.Default,
		key = AppSettings.KEY_WEBTOON_ZOOM,
		valueProducer = { isWebtoonZoomEnable },
	)

	val isZoomControlEnabled = settings.observeAsStateFlow(
		scope = viewModelScope + Dispatchers.Default,
		key = AppSettings.KEY_READER_ZOOM_BUTTONS,
		valueProducer = { isReaderZoomButtonsEnabled },
	)

	val readerSettings = ReaderSettings(
		parentScope = viewModelScope,
		settings = settings,
		colorFilterFlow = mangaFlow.flatMapLatest {
			if (it == null) flowOf(null) else dataRepository.observeColorFilter(it.id)
		}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, null),
	)

	val isScreenshotsBlockEnabled = combine(
		mangaFlow,
		settings.observeAsFlow(AppSettings.KEY_SCREENSHOTS_POLICY) { screenshotsPolicy },
	) { manga, policy ->
		policy == ScreenshotsPolicy.BLOCK_ALL ||
			(policy == ScreenshotsPolicy.BLOCK_NSFW && manga != null && manga.isNsfw)
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Lazily, false)

	val isBookmarkAdded = currentState.flatMapLatest { state ->
		val manga = mangaData.value?.any
		if (state == null || manga == null) {
			flowOf(false)
		} else {
			bookmarksRepository.observeBookmark(manga, state.chapterId, state.page)
				.map {
					it != null && it.chapterId == state.chapterId && it.page == state.page
				}
		}
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, false)

	init {
		loadImpl()
		settings.observe()
			.onEach { key ->
				if (key == AppSettings.KEY_READER_SLIDER) notifyStateChanged()
			}.launchIn(viewModelScope + Dispatchers.Default)
		launchJob(Dispatchers.Default) {
			val mangaId = mangaFlow.filterNotNull().first().id
			appShortcutManager.notifyMangaOpened(mangaId)
		}
	}

	fun reload() {
		loadingJob?.cancel()
		loadImpl()
	}

	fun switchMode(newMode: ReaderMode) {
		launchJob {
			val manga = checkNotNull(mangaData.value?.any)
			dataRepository.saveReaderMode(
				manga = manga,
				mode = newMode,
			)
			readerMode.value = newMode
			content.update {
				it.copy(state = getCurrentState())
			}
		}
	}

	fun saveCurrentState(state: ReaderState? = null) {
		if (state != null) {
			currentState.value = state
		}
		if (isIncognito) {
			return
		}
		val readerState = state ?: currentState.value ?: return
		historyUpdateUseCase.invokeAsync(
			manga = mangaData.value?.any ?: return,
			readerState = readerState,
			percent = computePercent(readerState.chapterId, readerState.page),
		)
	}

	fun getCurrentState() = currentState.value

	fun getCurrentChapterPages(): List<MangaPage>? {
		val chapterId = currentState.value?.chapterId ?: return null
		return chaptersLoader.getPages(chapterId).map { it.toMangaPage() }
	}

	fun saveCurrentPage(
		page: MangaPage,
		saveLauncher: ActivityResultLauncher<String>,
	) {
		val prevJob = pageSaveJob
		pageSaveJob = launchLoadingJob(Dispatchers.Default) {
			prevJob?.cancelAndJoin()
			try {
				val dest = pageSaveHelper.savePage(pageLoader, page, saveLauncher)
				onPageSaved.call(dest)
			} catch (e: CancellationException) {
				throw e
			} catch (e: Exception) {
				e.printStackTraceDebug()
				onPageSaved.call(null)
			}
		}
	}

	fun onActivityResult(uri: Uri?) {
		if (uri != null) {
			pageSaveHelper.onActivityResult(uri)
		} else {
			pageSaveJob?.cancel()
			pageSaveJob = null
		}
	}

	fun getCurrentPage(): MangaPage? {
		val state = currentState.value ?: return null
		return content.value.pages.find {
			it.chapterId == state.chapterId && it.index == state.page
		}?.toMangaPage()
	}

	fun switchChapter(id: Long, page: Int) {
		val prevJob = loadingJob
		loadingJob = launchLoadingJob(Dispatchers.Default) {
			prevJob?.cancelAndJoin()
			content.value = ReaderContent(emptyList(), null)
			chaptersLoader.loadSingleChapter(id)
			content.value = ReaderContent(chaptersLoader.snapshot(), ReaderState(id, page, 0))
		}
	}

	@MainThread
	fun onCurrentPageChanged(position: Int) {
		val prevJob = stateChangeJob
		stateChangeJob = launchJob(Dispatchers.Default) {
			prevJob?.cancelAndJoin()
			loadingJob?.join()
			val pages = content.value.pages
			pages.getOrNull(position)?.let { page ->
				currentState.update { cs ->
					cs?.copy(chapterId = page.chapterId, page = page.index)
				}
			}
			notifyStateChanged()
			if (pages.isEmpty() || loadingJob?.isActive == true) {
				return@launchJob
			}
			ensureActive()
			if (position >= pages.lastIndex - BOUNDS_PAGE_OFFSET) {
				loadPrevNextChapter(pages.last().chapterId, isNext = true)
			}
			if (position <= BOUNDS_PAGE_OFFSET) {
				loadPrevNextChapter(pages.first().chapterId, isNext = false)
			}
			if (pageLoader.isPrefetchApplicable()) {
				pageLoader.prefetch(pages.trySublist(position + 1, position + PREFETCH_LIMIT))
			}
		}
	}

	fun addBookmark() {
		if (bookmarkJob?.isActive == true) {
			return
		}
		bookmarkJob = launchJob(Dispatchers.Default) {
			loadingJob?.join()
			val state = checkNotNull(currentState.value)
			val page = checkNotNull(getCurrentPage()) { "Page not found" }
			val bookmark = Bookmark(
				manga = checkNotNull(mangaData.value?.any),
				pageId = page.id,
				chapterId = state.chapterId,
				page = state.page,
				scroll = state.scroll,
				imageUrl = page.preview.ifNullOrEmpty { page.url },
				createdAt = Date(),
				percent = computePercent(state.chapterId, state.page),
			)
			bookmarksRepository.addBookmark(bookmark)
			onShowToast.call(R.string.bookmark_added)
		}
	}

	fun removeBookmark() {
		if (bookmarkJob?.isActive == true) {
			return
		}
		bookmarkJob = launchJob {
			loadingJob?.join()
			val manga = checkNotNull(mangaData.value?.any)
			val state = checkNotNull(getCurrentState())
			bookmarksRepository.removeBookmark(manga.id, state.chapterId, state.page)
			onShowToast.call(R.string.bookmark_removed)
		}
	}

	private fun loadImpl() {
		loadingJob = launchLoadingJob(Dispatchers.Default) {
			var manga = DoubleManga(
				dataRepository.resolveIntent(intent)
					?: throw NotFoundException("Cannot find manga", ""),
			)
			mangaData.value = manga
			val mangaFlow = doubleMangaLoadUseCase(intent)
			manga = mangaFlow.first { x -> x.any != null }
			chaptersLoader.init(viewModelScope, mangaFlow)
			// determine mode
			val singleManga = manga.requireAny()
			// obtain state
			if (currentState.value == null) {
				currentState.value = historyRepository.getOne(singleManga)?.let {
					ReaderState(it)
				} ?: ReaderState(singleManga, preselectedBranch)
			}
			val mode = detectReaderModeUseCase.invoke(singleManga, currentState.value)
			val branch = chaptersLoader.awaitChapter(currentState.value?.chapterId ?: 0L)?.branch
			mangaData.value = manga.filterChapters(branch)
			readerMode.value = mode

			chaptersLoader.loadSingleChapter(requireNotNull(currentState.value).chapterId)
			// save state
			if (!isIncognito) {
				currentState.value?.let {
					val percent = computePercent(it.chapterId, it.page)
					historyUpdateUseCase.invoke(singleManga, it, percent)
				}
			}
			notifyStateChanged()
			content.value = ReaderContent(chaptersLoader.snapshot(), currentState.value)
		}
	}

	@AnyThread
	private fun loadPrevNextChapter(currentId: Long, isNext: Boolean) {
		val prevJob = loadingJob
		loadingJob = launchLoadingJob(Dispatchers.Default) {
			prevJob?.join()
			chaptersLoader.loadPrevNextChapter(mangaData.requireValue(), currentId, isNext)
			content.value = ReaderContent(chaptersLoader.snapshot(), null)
		}
	}

	private fun <T> List<T>.trySublist(fromIndex: Int, toIndex: Int): List<T> {
		val fromIndexBounded = fromIndex.coerceAtMost(lastIndex)
		val toIndexBounded = toIndex.coerceIn(fromIndexBounded, lastIndex)
		return if (fromIndexBounded == toIndexBounded) {
			emptyList()
		} else {
			subList(fromIndexBounded, toIndexBounded)
		}
	}

	@WorkerThread
	private fun notifyStateChanged() {
		val state = getCurrentState()
		val chapter = state?.chapterId?.let { chaptersLoader.peekChapter(it) }
		val newState = ReaderUiState(
			mangaName = manga?.any?.title,
			branch = chapter?.branch,
			chapterName = chapter?.name,
			chapterNumber = chapter?.number ?: 0,
			chaptersTotal = manga?.any?.getChapters(chapter?.branch)?.size ?: 0,
			totalPages = if (chapter != null) chaptersLoader.getPagesCount(chapter.id) else 0,
			currentPage = state?.page ?: 0,
			isSliderEnabled = settings.isReaderSliderEnabled,
			percent = if (state != null) computePercent(state.chapterId, state.page) else PROGRESS_NONE,
		)
		uiState.value = newState
	}

	private fun computePercent(chapterId: Long, pageIndex: Int): Float {
		val branch = chaptersLoader.peekChapter(chapterId)?.branch
		val chapters = manga?.any?.getChapters(branch) ?: return PROGRESS_NONE
		val chaptersCount = chapters.size
		val chapterIndex = chapters.indexOfFirst { x -> x.id == chapterId }
		val pagesCount = chaptersLoader.getPagesCount(chapterId)
		if (chaptersCount == 0 || pagesCount == 0) {
			return PROGRESS_NONE
		}
		val pagePercent = (pageIndex + 1) / pagesCount.toFloat()
		val ppc = 1f / chaptersCount
		return ppc * chapterIndex + ppc * pagePercent
	}
}
