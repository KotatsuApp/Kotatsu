package org.koitharu.kotatsu.reader.ui

import android.net.Uri
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.bookmarks.domain.Bookmark
import org.koitharu.kotatsu.bookmarks.domain.BookmarksRepository
import org.koitharu.kotatsu.core.model.getPreferredBranch
import org.koitharu.kotatsu.core.nav.MangaIntent
import org.koitharu.kotatsu.core.nav.ReaderIntent
import org.koitharu.kotatsu.core.os.AppShortcutManager
import org.koitharu.kotatsu.core.parser.MangaDataRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ReaderMode
import org.koitharu.kotatsu.core.prefs.observeAsFlow
import org.koitharu.kotatsu.core.prefs.observeAsStateFlow
import org.koitharu.kotatsu.core.util.ext.MutableEventFlow
import org.koitharu.kotatsu.core.util.ext.call
import org.koitharu.kotatsu.core.util.ext.ifNullOrEmpty
import org.koitharu.kotatsu.core.util.ext.requireValue
import org.koitharu.kotatsu.core.util.ext.sizeOrZero
import org.koitharu.kotatsu.details.data.MangaDetails
import org.koitharu.kotatsu.details.domain.DetailsInteractor
import org.koitharu.kotatsu.details.domain.DetailsLoadUseCase
import org.koitharu.kotatsu.details.ui.pager.ChaptersPagesSheet.Companion.TAB_PAGES
import org.koitharu.kotatsu.details.ui.pager.ChaptersPagesViewModel
import org.koitharu.kotatsu.download.ui.worker.DownloadWorker
import org.koitharu.kotatsu.history.data.HistoryRepository
import org.koitharu.kotatsu.history.domain.HistoryUpdateUseCase
import org.koitharu.kotatsu.list.domain.ReadingProgress.Companion.PROGRESS_NONE
import org.koitharu.kotatsu.local.data.LocalStorageChanges
import org.koitharu.kotatsu.local.domain.DeleteLocalMangaUseCase
import org.koitharu.kotatsu.local.domain.model.LocalManga
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.reader.domain.ChaptersLoader
import org.koitharu.kotatsu.reader.domain.DetectReaderModeUseCase
import org.koitharu.kotatsu.reader.domain.PageLoader
import org.koitharu.kotatsu.reader.ui.config.ReaderSettings
import org.koitharu.kotatsu.reader.ui.pager.ReaderUiState
import org.koitharu.kotatsu.stats.domain.StatsCollector
import java.time.Instant
import javax.inject.Inject

private const val BOUNDS_PAGE_OFFSET = 2
private const val PREFETCH_LIMIT = 10

@HiltViewModel
class ReaderViewModel @Inject constructor(
	private val savedStateHandle: SavedStateHandle,
	private val dataRepository: MangaDataRepository,
	private val historyRepository: HistoryRepository,
	private val bookmarksRepository: BookmarksRepository,
	settings: AppSettings,
	private val pageLoader: PageLoader,
	private val chaptersLoader: ChaptersLoader,
	private val appShortcutManager: AppShortcutManager,
	private val detailsLoadUseCase: DetailsLoadUseCase,
	private val historyUpdateUseCase: HistoryUpdateUseCase,
	private val detectReaderModeUseCase: DetectReaderModeUseCase,
	private val statsCollector: StatsCollector,
	@LocalStorageChanges localStorageChanges: SharedFlow<LocalManga?>,
	interactor: DetailsInteractor,
	deleteLocalMangaUseCase: DeleteLocalMangaUseCase,
	downloadScheduler: DownloadWorker.Scheduler,
) : ChaptersPagesViewModel(
	settings = settings,
	interactor = interactor,
	bookmarksRepository = bookmarksRepository,
	historyRepository = historyRepository,
	downloadScheduler = downloadScheduler,
	deleteLocalMangaUseCase = deleteLocalMangaUseCase,
	localStorageChanges = localStorageChanges,
) {
	private val intent = MangaIntent(savedStateHandle)

	private var loadingJob: Job? = null
	private var pageSaveJob: Job? = null
	private var bookmarkJob: Job? = null
	private var stateChangeJob: Job? = null

	init {
		selectedBranch.value = savedStateHandle.get<String>(ReaderIntent.EXTRA_BRANCH)
		readingState.value = savedStateHandle[ReaderIntent.EXTRA_STATE]
		mangaDetails.value = intent.manga?.let { MangaDetails(it, null, null, false) }
	}

	val readerMode = MutableStateFlow<ReaderMode?>(null)
	val onPageSaved = MutableEventFlow<Collection<Uri>>()
	val onShowToast = MutableEventFlow<Int>()
	val uiState = MutableStateFlow<ReaderUiState?>(null)

	val incognitoMode = if (savedStateHandle.get<Boolean>(ReaderIntent.EXTRA_INCOGNITO) == true) {
		MutableStateFlow(true)
	} else {
		interactor.observeIncognitoMode(manga)
			.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, false)
	}

	val isPagesSheetEnabled = observeIsPagesSheetEnabled()

	val content = MutableStateFlow(ReaderContent(emptyList(), null))

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

	val isKeepScreenOnEnabled = settings.observeAsStateFlow(
		scope = viewModelScope + Dispatchers.Default,
		key = AppSettings.KEY_READER_SCREEN_ON,
		valueProducer = { isReaderKeepScreenOn },
	)

	val isWebtoonZooEnabled = observeIsWebtoonZoomEnabled()
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Lazily, false)

	val isWebtoonGapsEnabled = settings.observeAsStateFlow(
		scope = viewModelScope + Dispatchers.Default,
		key = AppSettings.KEY_WEBTOON_GAPS,
		valueProducer = { isWebtoonGapsEnabled },
	)

	val defaultWebtoonZoomOut = observeIsWebtoonZoomEnabled().flatMapLatest {
		if (it) {
			observeWebtoonZoomOut()
		} else {
			flowOf(0f)
		}
	}.flowOn(Dispatchers.Default)

	val isZoomControlsEnabled = getObserveIsZoomControlEnabled().flatMapLatest { zoom ->
		if (zoom) {
			combine(readerMode, isWebtoonZooEnabled) { mode, ze -> ze || mode != ReaderMode.WEBTOON }
		} else {
			flowOf(false)
		}
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Lazily, false)

	val readerSettings = ReaderSettings(
		parentScope = viewModelScope,
		settings = settings,
		colorFilterFlow = manga.flatMapLatest {
			if (it == null) flowOf(null) else dataRepository.observeColorFilter(it.id)
		}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, null),
	)

	val isMangaNsfw = manga.map { it?.isNsfw == true }

	val isBookmarkAdded = readingState.flatMapLatest { state ->
		val manga = mangaDetails.value?.toManga()
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
		launchJob(Dispatchers.Default) {
			val mangaId = manga.filterNotNull().first().id
			appShortcutManager.notifyMangaOpened(mangaId)
		}
	}

	fun reload() {
		loadingJob?.cancel()
		loadImpl()
	}

	fun onPause() {
		getMangaOrNull()?.let {
			statsCollector.onPause(it.id)
		}
	}

	fun switchMode(newMode: ReaderMode) {
		launchJob {
			val manga = checkNotNull(getMangaOrNull())
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
			readingState.value = state
			savedStateHandle[ReaderIntent.EXTRA_STATE] = state
		}
		if (incognitoMode.value) {
			return
		}
		val readerState = state ?: readingState.value ?: return
		historyUpdateUseCase.invokeAsync(
			manga = getMangaOrNull() ?: return,
			readerState = readerState,
			percent = computePercent(readerState.chapterId, readerState.page),
		)
	}

	fun getCurrentState() = readingState.value

	fun getCurrentChapterPages(): List<MangaPage>? {
		val chapterId = readingState.value?.chapterId ?: return null
		return chaptersLoader.getPages(chapterId)
	}

	fun saveCurrentPage(
		pageSaveHelper: PageSaveHelper
	) {
		val prevJob = pageSaveJob
		pageSaveJob = launchLoadingJob(Dispatchers.Default) {
			prevJob?.cancelAndJoin()
			val state = checkNotNull(getCurrentState())
			val currentManga = manga.requireValue()
			val task = PageSaveHelper.Task(
				manga = currentManga,
				chapter = currentManga.requireChapterById(state.chapterId),
				pageNumber = state.page + 1,
				page = checkNotNull(getCurrentPage()) { "Cannot find current page" },
			)
			val dest = pageSaveHelper.save(setOf(task))
			onPageSaved.call(dest)
		}
	}

	fun getCurrentPage(): MangaPage? {
		val state = readingState.value ?: return null
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
			val newState = ReaderState(id, page, 0)
			content.value = ReaderContent(chaptersLoader.snapshot(), newState)
			saveCurrentState(newState)
		}
	}

	fun switchChapterBy(delta: Int) {
		val prevJob = loadingJob
		loadingJob = launchLoadingJob(Dispatchers.Default) {
			prevJob?.cancelAndJoin()
			val prevState = readingState.requireValue()
			val newChapterId = if (delta != 0) {
				val allChapters = mangaDetails.requireValue().allChapters
				var index = allChapters.indexOfFirst { x -> x.id == prevState.chapterId }
				if (index < 0) {
					return@launchLoadingJob
				}
				index += delta
				(allChapters.getOrNull(index) ?: return@launchLoadingJob).id
			} else {
				prevState.chapterId
			}
			content.value = ReaderContent(emptyList(), null)
			chaptersLoader.loadSingleChapter(newChapterId)
			val newState = ReaderState(
				chapterId = newChapterId,
				page = if (delta == 0) prevState.page else 0,
				scroll = if (delta == 0) prevState.scroll else 0,
			)
			content.value = ReaderContent(chaptersLoader.snapshot(), newState)
			saveCurrentState(newState)
		}
	}

	@MainThread
	fun onCurrentPageChanged(lowerPos: Int, upperPos: Int) {
		val prevJob = stateChangeJob
		val pages = content.value.pages // capture immediately
		stateChangeJob = launchJob(Dispatchers.Default) {
			prevJob?.cancelAndJoin()
			loadingJob?.join()
			if (pages.size != content.value.pages.size) {
				return@launchJob // TODO
			}
			val centerPos = (lowerPos + upperPos) / 2
			pages.getOrNull(centerPos)?.let { page ->
				readingState.update { cs ->
					cs?.copy(chapterId = page.chapterId, page = page.index)
				}
			}
			notifyStateChanged()
			if (pages.isEmpty() || loadingJob?.isActive == true) {
				return@launchJob
			}
			ensureActive()
			if (upperPos >= pages.lastIndex - BOUNDS_PAGE_OFFSET) {
				loadPrevNextChapter(pages.last().chapterId, isNext = true)
			}
			if (lowerPos <= BOUNDS_PAGE_OFFSET) {
				loadPrevNextChapter(pages.first().chapterId, isNext = false)
			}
			if (pageLoader.isPrefetchApplicable()) {
				pageLoader.prefetch(pages.trySublist(upperPos + 1, upperPos + PREFETCH_LIMIT))
			}
		}
	}

	fun addBookmark() {
		if (bookmarkJob?.isActive == true) {
			return
		}
		bookmarkJob = launchJob(Dispatchers.Default) {
			loadingJob?.join()
			val state = checkNotNull(readingState.value)
			val page = checkNotNull(getCurrentPage()) { "Page not found" }
			val bookmark = Bookmark(
				manga = requireManga(),
				pageId = page.id,
				chapterId = state.chapterId,
				page = state.page,
				scroll = state.scroll,
				imageUrl = page.preview.ifNullOrEmpty { page.url },
				createdAt = Instant.now(),
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
			val manga = requireManga()
			val state = checkNotNull(getCurrentState())
			bookmarksRepository.removeBookmark(manga.id, state.chapterId, state.page)
			onShowToast.call(R.string.bookmark_removed)
		}
	}

	private fun loadImpl() {
		loadingJob = launchLoadingJob(Dispatchers.Default) {
			val details = detailsLoadUseCase.invoke(intent).first { x -> x.isLoaded }
			mangaDetails.value = details
			chaptersLoader.init(details)
			val manga = details.toManga()
			// obtain state
			if (readingState.value == null) {
				readingState.value = getStateFromIntent(manga)
			}
			val mode = detectReaderModeUseCase.invoke(manga, readingState.value)
			val branch = chaptersLoader.peekChapter(readingState.value?.chapterId ?: 0L)?.branch
			selectedBranch.value = branch
			mangaDetails.value = details.filterChapters(branch)
			readerMode.value = mode

			chaptersLoader.loadSingleChapter(requireNotNull(readingState.value).chapterId)
			// save state
			if (!incognitoMode.value) {
				readingState.value?.let {
					val percent = computePercent(it.chapterId, it.page)
					historyUpdateUseCase.invoke(manga, it, percent)
				}
			}
			notifyStateChanged()
			content.value = ReaderContent(chaptersLoader.snapshot(), readingState.value)
		}
	}

	@AnyThread
	private fun loadPrevNextChapter(currentId: Long, isNext: Boolean) {
		val prevJob = loadingJob
		loadingJob = launchLoadingJob(Dispatchers.Default) {
			prevJob?.join()
			chaptersLoader.loadPrevNextChapter(mangaDetails.requireValue(), currentId, isNext)
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
		val state = getCurrentState() ?: return
		val chapter = chaptersLoader.peekChapter(state.chapterId) ?: return
		val m = mangaDetails.value ?: return
		val chapterIndex = m.chapters[chapter.branch]?.indexOfFirst { it.id == chapter.id } ?: -1
		val newState = ReaderUiState(
			mangaName = m.toManga().title,
			branch = chapter.branch,
			chapterName = chapter.name,
			chapterNumber = chapterIndex + 1,
			chaptersTotal = m.chapters[chapter.branch].sizeOrZero(),
			totalPages = chaptersLoader.getPagesCount(chapter.id),
			currentPage = state.page,
			percent = computePercent(state.chapterId, state.page),
			incognito = incognitoMode.value,
		)
		uiState.value = newState
		if (!incognitoMode.value) {
			statsCollector.onStateChanged(m.id, state)
		}
	}

	private fun computePercent(chapterId: Long, pageIndex: Int): Float {
		val branch = chaptersLoader.peekChapter(chapterId)?.branch
		val chapters = mangaDetails.value?.chapters?.get(branch) ?: return PROGRESS_NONE
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

	private fun observeIsWebtoonZoomEnabled() = settings.observeAsFlow(
		key = AppSettings.KEY_WEBTOON_ZOOM,
		valueProducer = { isWebtoonZoomEnabled },
	)

	private fun observeWebtoonZoomOut() = settings.observeAsFlow(
		key = AppSettings.KEY_WEBTOON_ZOOM_OUT,
		valueProducer = { defaultWebtoonZoomOut },
	)

	private fun getObserveIsZoomControlEnabled() = settings.observeAsFlow(
		key = AppSettings.KEY_READER_ZOOM_BUTTONS,
		valueProducer = { isReaderZoomButtonsEnabled },
	)

	private fun observeIsPagesSheetEnabled() = settings.observe()
		.filter { it == AppSettings.KEY_PAGES_TAB || it == AppSettings.KEY_DETAILS_TAB || it == AppSettings.KEY_DETAILS_LAST_TAB }
		.map { settings.defaultDetailsTab == TAB_PAGES }
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, settings.defaultDetailsTab == TAB_PAGES)

	private suspend fun getStateFromIntent(manga: Manga): ReaderState {
		val history = historyRepository.getOne(manga)
		val preselectedBranch = selectedBranch.value
		val result = if (history != null) {
			if (preselectedBranch != null && preselectedBranch != manga.findChapterById(history.chapterId)?.branch) {
				null
			} else {
				ReaderState(history)
			}
		} else {
			null
		}
		return result ?: ReaderState(manga, preselectedBranch ?: manga.getPreferredBranch(null))
	}
}
