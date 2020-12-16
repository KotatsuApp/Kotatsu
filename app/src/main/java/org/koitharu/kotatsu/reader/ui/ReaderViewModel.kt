package org.koitharu.kotatsu.reader.ui

import android.content.ContentResolver
import android.net.Uri
import android.util.LongSparseArray
import android.webkit.URLUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koitharu.kotatsu.base.domain.MangaDataRepository
import org.koitharu.kotatsu.base.domain.MangaIntent
import org.koitharu.kotatsu.base.domain.MangaUtils
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.exceptions.MangaNotFoundException
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaChapter
import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ReaderMode
import org.koitharu.kotatsu.history.domain.HistoryRepository
import org.koitharu.kotatsu.reader.ui.pager.ReaderPage
import org.koitharu.kotatsu.reader.ui.pager.ReaderUiState
import org.koitharu.kotatsu.utils.MediaStoreCompat
import org.koitharu.kotatsu.utils.SingleLiveEvent
import org.koitharu.kotatsu.utils.ext.*

class ReaderViewModel(
	intent: MangaIntent,
	state: ReaderState?,
	private val dataRepository: MangaDataRepository,
	private val historyRepository: HistoryRepository,
	private val settings: AppSettings
) : BaseViewModel() {

	private var loadingJob: Job? = null
	private val currentState = MutableStateFlow(state)
	private val mangaData = MutableStateFlow<Manga?>(intent.manga)
	private val chapters = LongSparseArray<MangaChapter>()

	val readerMode = MutableLiveData<ReaderMode>()
	val onPageSaved = SingleLiveEvent<Uri?>()
	val uiState = combine(
		mangaData,
		currentState
	) { manga, state ->
		val chapter = state?.chapterId?.let(chapters::get)
		ReaderUiState(
			mangaName = manga?.title,
			chapterName = chapter?.name,
			chapterNumber = chapter?.number ?: 0,
			chaptersTotal = chapters.size()
		)
	}.flowOn(Dispatchers.Default).asLiveData(viewModelScope.coroutineContext)

	val content = MutableLiveData<ReaderContent>(ReaderContent(emptyList(), null))
	val manga: Manga?
		get() = mangaData.value

	val readerAnimation = settings.observe()
		.filter { it == AppSettings.KEY_READER_ANIMATION }
		.map { settings.readerAnimation }
		.onStart { emit(settings.readerAnimation) }
		.distinctUntilChanged()
		.flowOn(Dispatchers.IO)
		.asLiveData(viewModelScope.coroutineContext)

	val onZoomChanged = settings.observe()
		.filter { it == AppSettings.KEY_ZOOM_MODE }
		.flowOn(Dispatchers.IO)
		.asLiveEvent(viewModelScope.coroutineContext)

	init {
		loadingJob = launchLoadingJob(Dispatchers.Default) {
			var manga = dataRepository.resolveIntent(intent)
				?: throw MangaNotFoundException("Cannot find manga")
			mangaData.value = manga
			val repo = manga.source.repository
			manga = repo.getDetails(manga)
			manga.chapters?.forEach {
				chapters.put(it.id, it)
			}
			mangaData.value = manga
			// determine mode
			val mode =
				dataRepository.getReaderMode(manga.id) ?: manga.chapters?.randomOrNull()?.let {
					val pages = repo.getPages(it)
					val isWebtoon = MangaUtils.determineMangaIsWebtoon(pages)
					val newMode = getReaderMode(isWebtoon)
					if (isWebtoon != null) {
						dataRepository.savePreferences(manga, newMode)
					}
					newMode
				} ?: error("There are no chapters in this manga")
			// obtain state
			if (state == null) {
				currentState.value = historyRepository.getOne(manga)?.let {
					ReaderState.from(it)
				} ?: ReaderState.initial(manga)
			}
			readerMode.postValue(mode)

			val pages = loadChapter(checkNotNull(manga.chapters?.firstOrNull()).id)
			content.postValue(ReaderContent(pages, currentState.value))
		}
	}

	fun switchMode(newMode: ReaderMode) {
		launchJob {
			val manga = checkNotNull(mangaData.value)
			dataRepository.savePreferences(
				manga = manga,
				mode = newMode
			)
			readerMode.value = newMode
		}
	}

	fun saveCurrentState(state: ReaderState? = null) {
		saveState(
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

	fun saveCurrentPage(resolver: ContentResolver) {
		launchJob(Dispatchers.Default) {
			try {
				val page =
					content.value?.pages?.randomOrNull()?.toMangaPage() ?: return@launchJob //TODO
				val repo = page.source.repository
				val url = repo.getPageFullUrl(page)
				val request = Request.Builder()
					.url(url)
					.get()
					.build()
				val uri = get<OkHttpClient>().newCall(request).await().use { response ->
					val fileName =
						URLUtil.guessFileName(
							url,
							response.contentDisposition,
							response.mimeType
						)
					MediaStoreCompat(resolver).insertImage(fileName) {
						checkNotNull(response.body).byteStream().copyTo(it)
					}
				}
				onPageSaved.postCall(uri)
			} catch (e: CancellationException) {
			} catch (e: Exception) {
				onPageSaved.postCall(null)
			}
		}
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
		pages.getOrNull(position)?.let {
			val currentValue = currentState.value
			if (currentValue != null && currentValue.chapterId != it.chapterId) {
				currentState.value = currentValue.copy(chapterId = it.chapterId)
			}
		}
		when {
			loadingJob?.isActive == true -> return
			pages.isEmpty() -> return
			position <= BOUNDS_PAGE_OFFSET -> {
				val chapterId = pages.first().chapterId
				loadPrevNextChapter(chapterId, -1)
			}
			position >= pages.size - BOUNDS_PAGE_OFFSET -> {
				val chapterId = pages.last().chapterId
				loadPrevNextChapter(chapterId, 1)
			}
		}
	}

	private fun getReaderMode(isWebtoon: Boolean?) = when {
		isWebtoon == true -> ReaderMode.WEBTOON
		settings.isPreferRtlReader -> ReaderMode.REVERSED
		else -> ReaderMode.STANDARD
	}

	private suspend fun loadChapter(chapterId: Long): List<ReaderPage> {
		val manga = checkNotNull(mangaData.value) { "Manga is null" }
		val chapter = checkNotNull(chapters.get(chapterId)) { "Chapter $chapterId not found" }
		val repo = manga.source.repository
		return repo.getPages(chapter).mapIndexed { index, page ->
			ReaderPage.from(page, index, chapterId)
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

	private companion object : KoinComponent {

		const val BOUNDS_PAGE_OFFSET = 2
		const val PAGES_TRIM_THRESHOLD = 120

		fun saveState(manga: Manga, state: ReaderState) {
			processLifecycleScope.launch(Dispatchers.Default + IgnoreErrors) {
				get<HistoryRepository>().addOrUpdate(
					manga = manga,
					chapterId = state.chapterId,
					page = state.page,
					scroll = state.scroll
				)
			}
		}

	}
}