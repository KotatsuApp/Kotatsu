package org.koitharu.kotatsu.reader.ui

import android.net.Uri
import android.util.LongSparseArray
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.ensureActive
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.domain.MangaDataRepository
import org.koitharu.kotatsu.base.domain.MangaIntent
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.bookmarks.domain.Bookmark
import org.koitharu.kotatsu.bookmarks.domain.BookmarksRepository
import org.koitharu.kotatsu.core.os.ShortcutsUpdater
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ReaderMode
import org.koitharu.kotatsu.core.prefs.ScreenshotsPolicy
import org.koitharu.kotatsu.core.prefs.observeAsFlow
import org.koitharu.kotatsu.core.prefs.observeAsLiveData
import org.koitharu.kotatsu.history.domain.HistoryRepository
import org.koitharu.kotatsu.history.domain.PROGRESS_NONE
import org.koitharu.kotatsu.parsers.exception.NotFoundException
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.reader.data.filterChapters
import org.koitharu.kotatsu.reader.domain.ChaptersLoader
import org.koitharu.kotatsu.reader.domain.PageLoader
import org.koitharu.kotatsu.reader.ui.config.ReaderSettings
import org.koitharu.kotatsu.reader.ui.pager.ReaderUiState
import org.koitharu.kotatsu.utils.SingleLiveEvent
import org.koitharu.kotatsu.utils.asFlowLiveData
import org.koitharu.kotatsu.utils.ext.emitValue
import org.koitharu.kotatsu.utils.ext.printStackTraceDebug
import org.koitharu.kotatsu.utils.ext.processLifecycleScope
import org.koitharu.kotatsu.utils.ext.requireValue
import org.koitharu.kotatsu.utils.ext.runCatchingCancellable
import java.util.Date
import javax.inject.Inject

private const val BOUNDS_PAGE_OFFSET = 2
private const val PREFETCH_LIMIT = 10

@HiltViewModel
class ReaderViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	private val mangaRepositoryFactory: MangaRepository.Factory,
	private val dataRepository: MangaDataRepository,
	private val historyRepository: HistoryRepository,
	private val bookmarksRepository: BookmarksRepository,
	private val settings: AppSettings,
	private val pageSaveHelper: PageSaveHelper,
	private val pageLoader: PageLoader,
	private val chaptersLoader: ChaptersLoader,
	private val shortcutsUpdater: ShortcutsUpdater,
) : BaseViewModel() {

	private val intent = MangaIntent(savedStateHandle)
	private val preselectedBranch = savedStateHandle.get<String>(ReaderActivity.EXTRA_BRANCH)
	private val isIncognito = savedStateHandle.get<Boolean>(ReaderActivity.EXTRA_INCOGNITO) ?: false

	private var loadingJob: Job? = null
	private var pageSaveJob: Job? = null
	private var bookmarkJob: Job? = null
	private var stateChangeJob: Job? = null
	private val currentState = MutableStateFlow<ReaderState?>(savedStateHandle[ReaderActivity.EXTRA_STATE])
	private val mangaData = MutableStateFlow(intent.manga)
	private val chapters: LongSparseArray<MangaChapter>
		get() = chaptersLoader.chapters

	val readerMode = MutableLiveData<ReaderMode>()
	val onPageSaved = SingleLiveEvent<Uri?>()
	val onShowToast = SingleLiveEvent<Int>()
	val uiState = MutableLiveData<ReaderUiState?>(null)

	val content = MutableLiveData(ReaderContent(emptyList(), null))
	val manga: Manga?
		get() = mangaData.value

	val readerAnimation = settings.observeAsLiveData(
		context = viewModelScope.coroutineContext + Dispatchers.Default,
		key = AppSettings.KEY_READER_ANIMATION,
		valueProducer = { readerAnimation },
	)

	val isInfoBarEnabled = settings.observeAsLiveData(
		context = viewModelScope.coroutineContext + Dispatchers.Default,
		key = AppSettings.KEY_READER_BAR,
		valueProducer = { isReaderBarEnabled },
	)

	val isWebtoonZoomEnabled = settings.observeAsLiveData(
		context = viewModelScope.coroutineContext + Dispatchers.Default,
		key = AppSettings.KEY_WEBTOON_ZOOM,
		valueProducer = { isWebtoonZoomEnable },
	)

	val readerSettings = ReaderSettings(
		parentScope = viewModelScope,
		settings = settings,
		colorFilterFlow = mangaData.flatMapLatest {
			if (it == null) flowOf(null) else dataRepository.observeColorFilter(it.id)
		}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, null),
	)

	val isScreenshotsBlockEnabled = combine(
		mangaData,
		settings.observeAsFlow(AppSettings.KEY_SCREENSHOTS_POLICY) { screenshotsPolicy },
	) { manga, policy ->
		policy == ScreenshotsPolicy.BLOCK_ALL ||
			(policy == ScreenshotsPolicy.BLOCK_NSFW && manga != null && manga.isNsfw)
	}.asFlowLiveData(viewModelScope.coroutineContext + Dispatchers.Default, false)

	val isBookmarkAdded: LiveData<Boolean> = currentState.flatMapLatest { state ->
		val manga = mangaData.value
		if (state == null || manga == null) {
			flowOf(false)
		} else {
			bookmarksRepository.observeBookmark(manga, state.chapterId, state.page)
				.map { it != null }
		}
	}.asFlowLiveData(viewModelScope.coroutineContext + Dispatchers.Default, false)

	init {
		loadImpl()
		settings.observe()
			.onEach { key ->
				if (key == AppSettings.KEY_READER_SLIDER) notifyStateChanged()
			}.launchIn(viewModelScope + Dispatchers.Default)
		launchJob(Dispatchers.Default) {
			val mangaId = mangaData.filterNotNull().first().id
			shortcutsUpdater.notifyMangaOpened(mangaId)
		}
	}

	fun reload() {
		loadingJob?.cancel()
		loadImpl()
	}

	fun switchMode(newMode: ReaderMode) {
		launchJob {
			val manga = checkNotNull(mangaData.value)
			dataRepository.saveReaderMode(
				manga = manga,
				mode = newMode,
			)
			readerMode.value = newMode
			content.value?.run {
				content.value = copy(
					state = getCurrentState(),
				)
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
		historyRepository.saveStateAsync(
			manga = mangaData.value ?: return,
			state = readerState,
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
				onPageSaved.emitCall(dest)
			} catch (e: CancellationException) {
				throw e
			} catch (e: Exception) {
				e.printStackTraceDebug()
				onPageSaved.emitCall(null)
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
		return content.value?.pages?.find {
			it.chapterId == state.chapterId && it.index == state.page
		}?.toMangaPage()
	}

	fun switchChapter(id: Long) {
		val prevJob = loadingJob
		loadingJob = launchLoadingJob(Dispatchers.Default) {
			prevJob?.cancelAndJoin()
			content.postValue(ReaderContent(emptyList(), null))
			chaptersLoader.loadSingleChapter(mangaData.requireValue(), id)
			content.postValue(ReaderContent(chaptersLoader.snapshot(), ReaderState(id, 0, 0)))
		}
	}

	@MainThread
	fun onCurrentPageChanged(position: Int) {
		val prevJob = stateChangeJob
		stateChangeJob = launchJob(Dispatchers.Default) {
			prevJob?.cancelAndJoin()
			loadingJob?.join()
			val pages = content.value?.pages ?: return@launchJob
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
				manga = checkNotNull(mangaData.value),
				pageId = page.id,
				chapterId = state.chapterId,
				page = state.page,
				scroll = state.scroll,
				imageUrl = page.preview ?: pageLoader.getPageUrl(page),
				createdAt = Date(),
				percent = computePercent(state.chapterId, state.page),
			)
			bookmarksRepository.addBookmark(bookmark)
			onShowToast.emitCall(R.string.bookmark_added)
		}
	}

	fun removeBookmark() {
		if (bookmarkJob?.isActive == true) {
			return
		}
		bookmarkJob = launchJob {
			loadingJob?.join()
			val manga = checkNotNull(mangaData.value)
			val page = checkNotNull(getCurrentPage()) { "Page not found" }
			bookmarksRepository.removeBookmark(manga.id, page.id)
			onShowToast.call(R.string.bookmark_removed)
		}
	}

	private fun loadImpl() {
		loadingJob = launchLoadingJob(Dispatchers.Default) {
			var manga = dataRepository.resolveIntent(intent) ?: throw NotFoundException("Cannot find manga", "")
			mangaData.value = manga
			val repo = mangaRepositoryFactory.create(manga.source)
			manga = repo.getDetails(manga)
			manga.chapters?.forEach {
				chapters.put(it.id, it)
			}
			// determine mode
			val mode = detectReaderMode(manga, repo)
			// obtain state
			if (currentState.value == null) {
				currentState.value = historyRepository.getOne(manga)?.let {
					ReaderState(it)
				} ?: ReaderState(manga, preselectedBranch)
			}

			val branch = chapters[currentState.value?.chapterId ?: 0L]?.branch
			mangaData.value = manga.filterChapters(branch)
			readerMode.emitValue(mode)

			chaptersLoader.loadSingleChapter(manga, requireNotNull(currentState.value).chapterId)
			// save state
			if (!isIncognito) {
				currentState.value?.let {
					val percent = computePercent(it.chapterId, it.page)
					historyRepository.addOrUpdate(manga, it.chapterId, it.page, it.scroll, percent)
				}
			}
			notifyStateChanged()
			content.emitValue(ReaderContent(chaptersLoader.snapshot(), currentState.value))
		}
	}

	@AnyThread
	private fun loadPrevNextChapter(currentId: Long, isNext: Boolean) {
		val prevJob = loadingJob
		loadingJob = launchLoadingJob(Dispatchers.Default) {
			prevJob?.join()
			chaptersLoader.loadPrevNextChapter(mangaData.requireValue(), currentId, isNext)
			content.emitValue(ReaderContent(chaptersLoader.snapshot(), null))
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

	private suspend fun detectReaderMode(manga: Manga, repo: MangaRepository): ReaderMode {
		dataRepository.getReaderMode(manga.id)?.let { return it }
		val defaultMode = settings.defaultReaderMode
		if (!settings.isReaderModeDetectionEnabled || defaultMode == ReaderMode.WEBTOON) {
			return defaultMode
		}
		val chapter = currentState.value?.chapterId?.let(chapters::get)
			?: manga.chapters?.randomOrNull()
			?: error("There are no chapters in this manga")
		val pages = repo.getPages(chapter)
		return runCatchingCancellable {
			val isWebtoon = dataRepository.determineMangaIsWebtoon(repo, pages)
			if (isWebtoon) ReaderMode.WEBTOON else defaultMode
		}.onSuccess {
			dataRepository.saveReaderMode(manga, it)
		}.onFailure {
			it.printStackTraceDebug()
		}.getOrDefault(defaultMode)
	}

	@WorkerThread
	private fun notifyStateChanged() {
		val state = getCurrentState()
		val chapter = state?.chapterId?.let(chapters::get)
		val newState = ReaderUiState(
			mangaName = manga?.title,
			chapterName = chapter?.name,
			chapterNumber = chapter?.number ?: 0,
			chaptersTotal = manga?.getChapters(chapter?.branch)?.size ?: 0,
			totalPages = if (chapter != null) chaptersLoader.getPagesCount(chapter.id) else 0,
			currentPage = state?.page ?: 0,
			isSliderEnabled = settings.isReaderSliderEnabled,
			percent = if (state != null) computePercent(state.chapterId, state.page) else PROGRESS_NONE,
		)
		uiState.postValue(newState)
	}

	private fun computePercent(chapterId: Long, pageIndex: Int): Float {
		val branch = chapters[chapterId]?.branch
		val chapters = manga?.getChapters(branch) ?: return PROGRESS_NONE
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

/**
 * This function is not a member of the ReaderViewModel
 * because it should work independently of the ViewModel's lifecycle.
 */
private fun HistoryRepository.saveStateAsync(manga: Manga, state: ReaderState, percent: Float): Job {
	return processLifecycleScope.launch(Dispatchers.Default) {
		runCatchingCancellable {
			addOrUpdate(
				manga = manga,
				chapterId = state.chapterId,
				page = state.page,
				scroll = state.scroll,
				percent = percent,
			)
		}.onFailure {
			it.printStackTraceDebug()
		}
	}
}
