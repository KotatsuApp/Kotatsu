package org.koitharu.kotatsu.reader.ui

import android.net.Uri
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
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
import org.koitharu.kotatsu.core.prefs.TriStateOption
import org.koitharu.kotatsu.core.prefs.observeAsFlow
import org.koitharu.kotatsu.core.prefs.observeAsStateFlow
import org.koitharu.kotatsu.core.util.ext.MutableEventFlow
import org.koitharu.kotatsu.core.util.ext.call
import org.koitharu.kotatsu.core.util.ext.firstNotNull
import org.koitharu.kotatsu.core.util.ext.requireValue
import org.koitharu.kotatsu.details.data.MangaDetails
import org.koitharu.kotatsu.details.domain.DetailsInteractor
import org.koitharu.kotatsu.details.domain.DetailsLoadUseCase
import org.koitharu.kotatsu.details.ui.pager.ChaptersPagesViewModel
import org.koitharu.kotatsu.download.ui.worker.DownloadWorker
import org.koitharu.kotatsu.history.data.HistoryRepository
import org.koitharu.kotatsu.history.domain.HistoryUpdateUseCase
import org.koitharu.kotatsu.list.domain.ReadingProgress.Companion.PROGRESS_NONE
import org.koitharu.kotatsu.local.data.LocalStorageChanges
import org.koitharu.kotatsu.local.domain.DeleteLocalMangaUseCase
import org.koitharu.kotatsu.local.domain.model.LocalManga
import org.koitharu.kotatsu.parsers.model.ContentRating
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.util.ifNullOrEmpty
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import org.koitharu.kotatsu.parsers.util.sizeOrZero
import org.koitharu.kotatsu.reader.domain.ChaptersLoader
import org.koitharu.kotatsu.reader.domain.DetectReaderModeUseCase
import org.koitharu.kotatsu.reader.domain.PageLoader
import org.koitharu.kotatsu.reader.ui.config.ReaderSettings
import org.koitharu.kotatsu.reader.ui.pager.ReaderUiState
import org.koitharu.kotatsu.scrobbling.discord.ui.DiscordRpc
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
	private val discordRpc: DiscordRpc,
	@LocalStorageChanges localStorageChanges: SharedFlow<LocalManga?>,
	interactor: DetailsInteractor,
	deleteLocalMangaUseCase: DeleteLocalMangaUseCase,
	downloadScheduler: DownloadWorker.Scheduler,
	readerSettingsProducerFactory: ReaderSettings.Producer.Factory,
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
		mangaDetails.value = intent.manga?.let { MangaDetails(it) }
	}

	val readerMode = MutableStateFlow<ReaderMode?>(null)
	val onPageSaved = MutableEventFlow<Collection<Uri>>()
	val onLoadingError = MutableEventFlow<Throwable>()
	val onShowToast = MutableEventFlow<Int>()
	val onAskNsfwIncognito = MutableEventFlow<Unit>()
	val uiState = MutableStateFlow<ReaderUiState?>(null)

	val isIncognitoMode = MutableStateFlow(savedStateHandle.get<Boolean>(ReaderIntent.EXTRA_INCOGNITO))

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

	val isInfoBarTransparent = settings.observeAsStateFlow(
		scope = viewModelScope + Dispatchers.Default,
		key = AppSettings.KEY_READER_BAR_TRANSPARENT,
		valueProducer = { isReaderBarTransparent },
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

	val isWebtoonPullGestureEnabled = settings.observeAsStateFlow(
		scope = viewModelScope + Dispatchers.Default,
		key = AppSettings.KEY_WEBTOON_PULL_GESTURE,
		valueProducer = { isWebtoonPullGestureEnabled },
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

	val readerSettingsProducer = readerSettingsProducerFactory.create(
		manga.mapNotNull { it?.id },
	)

	val isMangaNsfw = manga.map { it?.contentRating == ContentRating.ADULT }

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
		initIncognitoMode()
		loadImpl()
		launchJob(Dispatchers.Default) {
			val mangaId = manga.filterNotNull().first().id
			if (!isIncognitoMode.firstNotNull()) {
				appShortcutManager.notifyMangaOpened(mangaId)
			}
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

	fun onStop() {
		discordRpc.clearRpc()
	}

	fun onIdle() {
		discordRpc.setIdle()
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
		if (isIncognitoMode.value != false) {
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
				chapterId = state.chapterId,
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
			val autoLoadAllowed = readerMode.value != ReaderMode.WEBTOON || !isWebtoonPullGestureEnabled.value
			if (autoLoadAllowed) {
				if (upperPos >= pages.lastIndex - BOUNDS_PAGE_OFFSET) {
					loadPrevNextChapter(pages.last().chapterId, isNext = true)
				}
				if (lowerPos <= BOUNDS_PAGE_OFFSET) {
					loadPrevNextChapter(pages.first().chapterId, isNext = false)
				}
			}
			if (pageLoader.isPrefetchApplicable()) {
				pageLoader.prefetch(pages.trySublist(upperPos + 1, upperPos + PREFETCH_LIMIT))
			}
		}
	}

	fun toggleBookmark() {
		if (bookmarkJob?.isActive == true) {
			return
		}
		bookmarkJob = launchJob(Dispatchers.Default) {
			loadingJob?.join()
			val state = checkNotNull(getCurrentState())
			if (isBookmarkAdded.value) {
				val manga = requireManga()
				bookmarksRepository.removeBookmark(manga.id, state.chapterId, state.page)
				onShowToast.call(R.string.bookmark_removed)
			} else {
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
	}

	fun setIncognitoMode(value: Boolean, dontAskAgain: Boolean) {
		isIncognitoMode.value = value
		if (dontAskAgain) {
			settings.incognitoModeForNsfw = if (value) TriStateOption.ENABLED else TriStateOption.DISABLED
		}
	}

	private fun loadImpl() {
		loadingJob = launchLoadingJob(Dispatchers.Default + EventExceptionHandler(onLoadingError)) {
			var exception: Exception? = null
			try {
				detailsLoadUseCase(intent, force = false)
					.collect { details ->
						if (mangaDetails.value == null) {
							mangaDetails.value = details
						}
						chaptersLoader.init(details)
						val manga = details.toManga()
						// obtain state
						if (readingState.value == null) {
							val newState = getStateFromIntent(manga)
							if (newState == null) {
								return@collect // manga not loaded yet if cannot get state
							}
							readingState.value = newState
							val mode = runCatchingCancellable {
								detectReaderModeUseCase(manga, newState)
							}.getOrDefault(settings.defaultReaderMode)
							val branch = chaptersLoader.peekChapter(newState.chapterId)?.branch
							selectedBranch.value = branch
							readerMode.value = mode
							try {
								chaptersLoader.loadSingleChapter(newState.chapterId)
							} catch (e: Exception) {
								readingState.value = null // try next time
								exception = e.mergeWith(exception)
								return@collect
							}
						}
						mangaDetails.value = details.filterChapters(selectedBranch.value)

						// save state
						if (!isIncognitoMode.firstNotNull()) {
							readingState.value?.let {
								val percent = computePercent(it.chapterId, it.page)
								historyUpdateUseCase(manga, it, percent)
							}
						}
						notifyStateChanged()
						content.value = ReaderContent(chaptersLoader.snapshot(), readingState.value)
					}
			} catch (e: CancellationException) {
				throw e
			} catch (e: Exception) {
				exception = e.mergeWith(exception)
			}
			if (readingState.value == null) {
				onLoadingError.call(
					exception ?: IllegalStateException("Unable to load manga. This should never happen. Please report"),
				)
			} else exception?.let { e ->
				// manga has been loaded but error occurred
				errorEvent.call(e)
			}
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
			chapter = chapter,
			chapterIndex = chapterIndex,
			chaptersTotal = m.chapters[chapter.branch].sizeOrZero(),
			totalPages = chaptersLoader.getPagesCount(chapter.id),
			currentPage = state.page,
			percent = computePercent(state.chapterId, state.page),
			incognito = isIncognitoMode.value == true,
		)
		uiState.value = newState
		if (isIncognitoMode.value == false) {
			statsCollector.onStateChanged(m.id, state)
			discordRpc.updateRpc(m.toManga(), newState)
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

	private fun initIncognitoMode() {
		if (isIncognitoMode.value != null) {
			return
		}
		launchJob(Dispatchers.Default) {
			interactor.observeIncognitoMode(manga)
				.collect {
					when (it) {
						TriStateOption.ENABLED -> isIncognitoMode.value = true
						TriStateOption.ASK -> {
							onAskNsfwIncognito.call(Unit)
							return@collect
						}

						TriStateOption.DISABLED -> isIncognitoMode.value = false
					}
				}
		}
	}

	private suspend fun getStateFromIntent(manga: Manga): ReaderState? {
		// check if we have at least some chapters loaded
		if (manga.chapters.isNullOrEmpty()) {
			return null
		}
		// specific state is requested
		val requestedState: ReaderState? = savedStateHandle[ReaderIntent.EXTRA_STATE]
		if (requestedState != null) {
			return if (manga.findChapterById(requestedState.chapterId) != null) {
				requestedState
			} else {
				null
			}
		}

		val requestedBranch: String? = savedStateHandle[ReaderIntent.EXTRA_BRANCH]
		// continue reading
		val history = historyRepository.getOne(manga)
		if (history != null) {
			val chapter = manga.findChapterById(history.chapterId) ?: return null
			// specified branch is requested
			return if (ReaderIntent.EXTRA_BRANCH in savedStateHandle) {
				if (chapter.branch == requestedBranch) {
					ReaderState(history)
				} else {
					ReaderState(manga, requestedBranch)
				}
			} else {
				ReaderState(history)
			}
		}

		// start from beginning
		val preferredBranch = requestedBranch ?: manga.getPreferredBranch(null)
		return ReaderState(manga, preferredBranch)
	}

	private fun Exception.mergeWith(other: Exception?): Exception = if (other == null) {
		this
	} else {
		other.addSuppressed(this)
		other
	}
}
