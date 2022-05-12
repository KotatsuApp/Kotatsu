package org.koitharu.kotatsu.details.ui

import androidx.lifecycle.*
import java.io.IOException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.domain.MangaDataRepository
import org.koitharu.kotatsu.base.domain.MangaIntent
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.bookmarks.domain.Bookmark
import org.koitharu.kotatsu.bookmarks.domain.BookmarksRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.observeAsFlow
import org.koitharu.kotatsu.details.domain.BranchComparator
import org.koitharu.kotatsu.details.ui.model.ChapterListItem
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository
import org.koitharu.kotatsu.history.domain.HistoryRepository
import org.koitharu.kotatsu.local.domain.LocalMangaRepository
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.util.mapToSet
import org.koitharu.kotatsu.tracker.domain.TrackingRepository
import org.koitharu.kotatsu.utils.SingleLiveEvent
import org.koitharu.kotatsu.utils.ext.asLiveDataDistinct
import org.koitharu.kotatsu.utils.ext.printStackTraceDebug

class DetailsViewModel(
	intent: MangaIntent,
	private val historyRepository: HistoryRepository,
	favouritesRepository: FavouritesRepository,
	private val localMangaRepository: LocalMangaRepository,
	private val trackingRepository: TrackingRepository,
	mangaDataRepository: MangaDataRepository,
	private val bookmarksRepository: BookmarksRepository,
	private val settings: AppSettings,
) : BaseViewModel() {

	private val delegate = MangaDetailsDelegate(
		intent = intent,
		settings = settings,
		mangaDataRepository = mangaDataRepository,
		historyRepository = historyRepository,
		localMangaRepository = localMangaRepository,
	)

	private var loadingJob: Job

	val onShowToast = SingleLiveEvent<Int>()

	private val history = historyRepository.observeOne(delegate.mangaId)
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, null)

	private val favourite = favouritesRepository.observeCategoriesIds(delegate.mangaId).map { it.isNotEmpty() }
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, false)

	private val newChapters = viewModelScope.async(Dispatchers.Default) {
		trackingRepository.getNewChaptersCount(delegate.mangaId)
	}

	private val chaptersQuery = MutableStateFlow("")

	private val chaptersReversed = settings.observeAsFlow(AppSettings.KEY_REVERSE_CHAPTERS) { chaptersReverse }
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, false)

	val manga = delegate.manga.filterNotNull().asLiveData(viewModelScope.coroutineContext)
	val favouriteCategories = favourite.asLiveData(viewModelScope.coroutineContext)
	val newChaptersCount = liveData(viewModelScope.coroutineContext) { emit(newChapters.await()) }
	val readingHistory = history.asLiveData(viewModelScope.coroutineContext)
	val isChaptersReversed = chaptersReversed.asLiveData(viewModelScope.coroutineContext)

	val bookmarks = delegate.manga.flatMapLatest {
		if (it != null) bookmarksRepository.observeBookmarks(it) else flowOf(emptyList())
	}.asLiveDataDistinct(viewModelScope.coroutineContext + Dispatchers.Default)

	val onMangaRemoved = SingleLiveEvent<Manga>()

	val branches: LiveData<List<String?>> = delegate.manga.map {
		val chapters = it?.chapters ?: return@map emptyList()
		chapters.mapToSet { x -> x.branch }.sortedWith(BranchComparator())
	}.asLiveDataDistinct(viewModelScope.coroutineContext + Dispatchers.Default)

	val selectedBranchIndex = combine(
		branches.asFlow(),
		delegate.selectedBranch
	) { branches, selected ->
		branches.indexOf(selected)
	}.asLiveDataDistinct(viewModelScope.coroutineContext + Dispatchers.Default)

	val isChaptersEmpty = delegate.manga.map { m ->
		m?.chapters?.isEmpty() == true
	}.asLiveDataDistinct(viewModelScope.coroutineContext + Dispatchers.Default, false)

	val chapters = combine(
		combine(
			delegate.manga,
			delegate.relatedManga,
			history,
			delegate.selectedBranch,
		) { manga, related, history, branch ->
			delegate.mapChapters(manga, related, history, newChapters.await(), branch)
		},
		chaptersReversed,
		chaptersQuery,
	) { list, reversed, query ->
		(if (reversed) list.asReversed() else list).filterSearch(query)
	}.asLiveData(viewModelScope.coroutineContext + Dispatchers.Default)

	val selectedBranchValue: String?
		get() = delegate.selectedBranch.value

	init {
		loadingJob = doLoad()
	}

	fun reload() {
		loadingJob.cancel()
		loadingJob = doLoad()
	}

	fun deleteLocal() {
		val m = delegate.manga.value
		if (m == null) {
			onShowToast.call(R.string.file_not_found)
			return
		}
		launchLoadingJob(Dispatchers.Default) {
			val manga = if (m.source == MangaSource.LOCAL) m else localMangaRepository.findSavedManga(m)
			checkNotNull(manga) { "Cannot find saved manga for ${m.title}" }
			val original = localMangaRepository.getRemoteManga(manga)
			localMangaRepository.delete(manga) || throw IOException("Unable to delete file")
			runCatching {
				historyRepository.deleteOrSwap(manga, original)
			}
			onMangaRemoved.postCall(manga)
		}
	}

	fun removeBookmark(bookmark: Bookmark) {
		launchJob {
			bookmarksRepository.removeBookmark(bookmark.manga.id, bookmark.pageId)
			onShowToast.call(R.string.bookmark_removed)
		}
	}

	fun setChaptersReversed(newValue: Boolean) {
		settings.chaptersReverse = newValue
	}

	fun setSelectedBranch(branch: String?) {
		delegate.selectedBranch.value = branch
	}

	fun getRemoteManga(): Manga? {
		return delegate.relatedManga.value?.takeUnless { it.source == MangaSource.LOCAL }
	}

	fun performChapterSearch(query: String?) {
		chaptersQuery.value = query?.trim().orEmpty()
	}

	fun onDownloadComplete(downloadedManga: Manga) {
		val currentManga = delegate.manga.value ?: return
		if (currentManga.id != downloadedManga.id) {
			return
		}
		if (currentManga.source == MangaSource.LOCAL) {
			reload()
		} else {
			viewModelScope.launch(Dispatchers.Default) {
				runCatching {
					localMangaRepository.getDetails(downloadedManga)
				}.onSuccess {
					delegate.relatedManga.value = it
				}.onFailure {
					it.printStackTraceDebug()
				}
			}
		}
	}

	private fun doLoad() = launchLoadingJob(Dispatchers.Default) {
		delegate.doLoad()
	}

	private fun List<ChapterListItem>.filterSearch(query: String): List<ChapterListItem> {
		if (query.isEmpty() || this.isEmpty()) {
			return this
		}
		return filter {
			it.chapter.name.contains(query, ignoreCase = true)
		}
	}
}