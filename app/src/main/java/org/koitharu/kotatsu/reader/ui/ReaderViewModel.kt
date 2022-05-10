package org.koitharu.kotatsu.reader.ui

import android.net.Uri
import android.util.LongSparseArray
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.domain.MangaDataRepository
import org.koitharu.kotatsu.base.domain.MangaIntent
import org.koitharu.kotatsu.base.domain.MangaUtils
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.bookmarks.domain.Bookmark
import org.koitharu.kotatsu.bookmarks.domain.BookmarksRepository
import org.koitharu.kotatsu.core.exceptions.MangaNotFoundException
import org.koitharu.kotatsu.core.os.ShortcutsRepository
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.prefs.*
import org.koitharu.kotatsu.history.domain.HistoryRepository
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.reader.data.filterChapters
import org.koitharu.kotatsu.reader.domain.PageLoader
import org.koitharu.kotatsu.reader.ui.pager.ReaderPage
import org.koitharu.kotatsu.reader.ui.pager.ReaderUiState
import org.koitharu.kotatsu.utils.SingleLiveEvent
import org.koitharu.kotatsu.utils.ext.asLiveDataDistinct
import org.koitharu.kotatsu.utils.ext.printStackTraceDebug
import org.koitharu.kotatsu.utils.ext.processLifecycleScope
import java.util.*

private const val BOUNDS_PAGE_OFFSET = 2
private const val PAGES_TRIM_THRESHOLD = 120
private const val PREFETCH_LIMIT = 10

class ReaderViewModel(
	private val intent: MangaIntent,
	initialState: ReaderState?,
	private val preselectedBranch: String?,
	private val dataRepository: MangaDataRepository,
	private val historyRepository: HistoryRepository,
	private val shortcutsRepository: ShortcutsRepository,
	private val bookmarksRepository: BookmarksRepository,
	private val settings: AppSettings,
	private val pageSaveHelper: PageSaveHelper,
) : BaseViewModel() {

	private var loadingJob: Job? = null
	private var pageSaveJob: Job? = null
	private var bookmarkJob: Job? = null
	private val currentState = MutableStateFlow(initialState)
	private val mangaData = MutableStateFlow(intent.manga)
	private val chapters = LongSparseArray<MangaChapter>()

	val pageLoader = PageLoader()

	val readerMode = MutableLiveData<ReaderMode>()
	val onPageSaved = SingleLiveEvent<Uri?>()
	val onShowToast = SingleLiveEvent<Int>()
	val uiState = combine(
		mangaData,
		currentState,
	) { manga, state ->
		val chapter = state?.chapterId?.let(chapters::get)
		ReaderUiState(
			mangaName = manga?.title,
			chapterName = chapter?.name,
			chapterNumber = chapter?.number ?: 0,
			chaptersTotal = chapters.size()
		)
	}.asLiveDataDistinct(viewModelScope.coroutineContext + Dispatchers.Default)

	val content = MutableLiveData(ReaderContent(emptyList(), null))
	val manga: Manga?
		get() = mangaData.value

	val readerAnimation = settings.observeAsLiveData(
		context = viewModelScope.coroutineContext + Dispatchers.Default,
		key = AppSettings.KEY_READER_ANIMATION,
		valueProducer = { readerAnimation }
	)

	val isScreenshotsBlockEnabled = combine(
		mangaData,
		settings.observeAsFlow(AppSettings.KEY_SCREENSHOTS_POLICY) { screenshotsPolicy },
	) { manga, policy ->
		policy == ScreenshotsPolicy.BLOCK_ALL ||
			(policy == ScreenshotsPolicy.BLOCK_NSFW && manga != null && manga.isNsfw)
	}.asLiveDataDistinct(viewModelScope.coroutineContext + Dispatchers.Default)

	val onZoomChanged = SingleLiveEvent<Unit>()

	val isBookmarkAdded: LiveData<Boolean> = currentState.flatMapLatest { state ->
		val manga = mangaData.value
		if (state == null || manga == null) {
			flowOf(false)
		} else {
			bookmarksRepository.observeBookmark(manga, state.chapterId, state.page)
				.map { it != null }
		}
	}.asLiveDataDistinct(viewModelScope.coroutineContext + Dispatchers.Default)

	init {
		loadImpl()
		subscribeToSettings()
	}

	override fun onCleared() {
		pageLoader.close()
		super.onCleared()
	}

	fun reload() {
		loadingJob?.cancel()
		loadImpl()
	}

	fun switchMode(newMode: ReaderMode) {
		launchJob {
			val manga = checkNotNull(mangaData.value)
			dataRepository.savePreferences(
				manga = manga,
				mode = newMode
			)
			readerMode.value = newMode
			content.value?.run {
				content.value = copy(
					state = getCurrentState()
				)
			}
		}
	}

	fun saveCurrentState(state: ReaderState? = null) {
		if (state != null) {
			currentState.value = state
		}
		historyRepository.saveStateAsync(
			mangaData.value ?: return,
			state ?: currentState.value ?: return
		)
	}

	fun getCurrentState() = currentState.value

	fun getCurrentChapterPages(): List<MangaPage>? {
		val chapterId = currentState.value?.chapterId ?: return null
		val pages = content.value?.pages ?: return null
		return pages.filter { it.chapterId == chapterId }.map { it.toMangaPage() }
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
				onPageSaved.postCall(dest)
			} catch (e: CancellationException) {
				throw e
			} catch (e: Exception) {
				e.printStackTraceDebug()
				onPageSaved.postCall(null)
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
			val newPages = loadChapter(id)
			content.postValue(ReaderContent(newPages, ReaderState(id, 0, 0)))
		}
	}

	fun onCurrentPageChanged(position: Int) {
		val pages = content.value?.pages ?: return
		pages.getOrNull(position)?.let { page ->
			currentState.update { cs ->
				cs?.copy(chapterId = page.chapterId, page = page.index)
			}
		}
		if (pages.isEmpty() || loadingJob?.isActive == true) {
			return
		}
		if (position <= BOUNDS_PAGE_OFFSET) {
			loadPrevNextChapter(pages.first().chapterId, -1)
		}
		if (position >= pages.size - BOUNDS_PAGE_OFFSET) {
			loadPrevNextChapter(pages.last().chapterId, 1)
		}
		if (pageLoader.isPrefetchApplicable()) {
			pageLoader.prefetch(pages.trySublist(position + 1, position + PREFETCH_LIMIT))
		}
	}

	fun addBookmark() {
		if (bookmarkJob?.isActive == true) {
			return
		}
		bookmarkJob = launchJob {
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
			val manga = checkNotNull(mangaData.value)
			val page = checkNotNull(getCurrentPage()) { "Page not found" }
			bookmarksRepository.removeBookmark(manga.id, page.id)
			onShowToast.call(R.string.bookmark_removed)
		}
	}

	private fun loadImpl() {
		loadingJob = launchLoadingJob(Dispatchers.Default) {
			var manga = dataRepository.resolveIntent(intent) ?: throw MangaNotFoundException("Cannot find manga")
			mangaData.value = manga
			val repo = MangaRepository(manga.source)
			manga = repo.getDetails(manga)
			manga.chapters?.forEach {
				chapters.put(it.id, it)
			}
			// determine mode
			val mode = dataRepository.getReaderMode(manga.id) ?: manga.chapters?.randomOrNull()?.let {
				val pages = repo.getPages(it)
				val isWebtoon = MangaUtils.determineMangaIsWebtoon(pages)
				val newMode = getReaderMode(isWebtoon)
				if (isWebtoon != null) {
					dataRepository.savePreferences(manga, newMode)
				}
				newMode
			} ?: error("There are no chapters in this manga")
			// obtain state
			if (currentState.value == null) {
				currentState.value = historyRepository.getOne(manga)?.let {
					ReaderState(it)
				} ?: ReaderState(manga, preselectedBranch)
			}

			val branch = chapters[currentState.value?.chapterId ?: 0L].branch
			mangaData.value = manga.filterChapters(branch)
			readerMode.postValue(mode)

			val pages = loadChapter(requireNotNull(currentState.value).chapterId)
			// save state
			currentState.value?.let {
				historyRepository.addOrUpdate(manga, it.chapterId, it.page, it.scroll)
				shortcutsRepository.updateShortcuts()
			}

			content.postValue(ReaderContent(pages, currentState.value))
		}
	}

	private fun getReaderMode(isWebtoon: Boolean?) = when {
		isWebtoon == true -> ReaderMode.WEBTOON
		settings.isPreferRtlReader -> ReaderMode.REVERSED
		else -> ReaderMode.STANDARD
	}

	private suspend fun loadChapter(chapterId: Long): List<ReaderPage> {
		val manga = checkNotNull(mangaData.value) { "Manga is null" }
		val chapter = checkNotNull(chapters[chapterId]) { "Requested chapter not found" }
		val repo = MangaRepository(manga.source)
		return repo.getPages(chapter).mapIndexed { index, page ->
			ReaderPage(page, index, chapterId)
		}
	}

	private fun loadPrevNextChapter(currentId: Long, delta: Int) {
		loadingJob = launchLoadingJob(Dispatchers.Default) {
			val chapters = mangaData.value?.chapters ?: return@launchLoadingJob
			val predicate: (MangaChapter) -> Boolean = { it.id == currentId }
			val index =
				if (delta < 0) chapters.indexOfLast(predicate) else chapters.indexOfFirst(predicate)
			if (index == -1) return@launchLoadingJob
			val newChapter = chapters.getOrNull(index + delta) ?: return@launchLoadingJob
			val newPages = loadChapter(newChapter.id)
			var currentPages = content.value?.pages ?: return@launchLoadingJob
			// trim pages
			if (currentPages.size > PAGES_TRIM_THRESHOLD) {
				val firstChapterId = currentPages.first().chapterId
				val lastChapterId = currentPages.last().chapterId
				if (firstChapterId != lastChapterId) {
					currentPages = when (delta) {
						1 -> currentPages.dropWhile { it.chapterId == firstChapterId }
						-1 -> currentPages.dropLastWhile { it.chapterId == lastChapterId }
						else -> currentPages
					}
				}
			}
			val pages = when (delta) {
				0 -> newPages
				-1 -> newPages + currentPages
				1 -> currentPages + newPages
				else -> error("Invalid delta $delta")
			}
			content.postValue(ReaderContent(pages, null))
		}
	}

	private fun subscribeToSettings() {
		settings.observe()
			.onEach { key ->
				if (key == AppSettings.KEY_ZOOM_MODE) onZoomChanged.postCall(Unit)
			}.launchIn(viewModelScope + Dispatchers.Default)
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
}

/**
 * This function is not a member of the ReaderViewModel
 * because it should work independently of the ViewModel's lifecycle.
 */
private fun HistoryRepository.saveStateAsync(manga: Manga, state: ReaderState): Job {
	return processLifecycleScope.launch(Dispatchers.Default) {
		runCatching {
			addOrUpdate(
				manga = manga,
				chapterId = state.chapterId,
				page = state.page,
				scroll = state.scroll
			)
		}.onFailure {
			it.printStackTraceDebug()
		}
	}
}