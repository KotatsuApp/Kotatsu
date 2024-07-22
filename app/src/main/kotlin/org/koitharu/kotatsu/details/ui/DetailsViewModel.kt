package org.koitharu.kotatsu.details.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.plus
import okio.FileNotFoundException
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.bookmarks.domain.Bookmark
import org.koitharu.kotatsu.bookmarks.domain.BookmarksRepository
import org.koitharu.kotatsu.core.model.findById
import org.koitharu.kotatsu.core.model.getPreferredBranch
import org.koitharu.kotatsu.core.parser.MangaIntent
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.core.prefs.observeAsStateFlow
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.ui.util.ReversibleAction
import org.koitharu.kotatsu.core.util.ext.MutableEventFlow
import org.koitharu.kotatsu.core.util.ext.call
import org.koitharu.kotatsu.core.util.ext.combine
import org.koitharu.kotatsu.core.util.ext.computeSize
import org.koitharu.kotatsu.core.util.ext.onEachWhile
import org.koitharu.kotatsu.core.util.ext.requireValue
import org.koitharu.kotatsu.details.data.MangaDetails
import org.koitharu.kotatsu.details.domain.BranchComparator
import org.koitharu.kotatsu.details.domain.DetailsInteractor
import org.koitharu.kotatsu.details.domain.DetailsLoadUseCase
import org.koitharu.kotatsu.details.domain.ProgressUpdateUseCase
import org.koitharu.kotatsu.details.domain.ReadingTimeUseCase
import org.koitharu.kotatsu.details.domain.RelatedMangaUseCase
import org.koitharu.kotatsu.details.ui.model.ChapterListItem
import org.koitharu.kotatsu.details.ui.model.HistoryInfo
import org.koitharu.kotatsu.details.ui.model.MangaBranch
import org.koitharu.kotatsu.download.ui.worker.DownloadWorker
import org.koitharu.kotatsu.history.data.HistoryRepository
import org.koitharu.kotatsu.list.domain.MangaListMapper
import org.koitharu.kotatsu.list.ui.model.MangaListModel
import org.koitharu.kotatsu.local.data.LocalStorageChanges
import org.koitharu.kotatsu.local.domain.DeleteLocalMangaUseCase
import org.koitharu.kotatsu.local.domain.model.LocalManga
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import org.koitharu.kotatsu.scrobbling.common.domain.Scrobbler
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblingInfo
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblingStatus
import org.koitharu.kotatsu.stats.data.StatsRepository
import javax.inject.Inject

@HiltViewModel
class DetailsViewModel @Inject constructor(
	private val historyRepository: HistoryRepository,
	private val bookmarksRepository: BookmarksRepository,
	private val settings: AppSettings,
	private val scrobblers: Set<@JvmSuppressWildcards Scrobbler>,
	@LocalStorageChanges private val localStorageChanges: SharedFlow<LocalManga?>,
	private val downloadScheduler: DownloadWorker.Scheduler,
	private val interactor: DetailsInteractor,
	savedStateHandle: SavedStateHandle,
	private val deleteLocalMangaUseCase: DeleteLocalMangaUseCase,
	private val relatedMangaUseCase: RelatedMangaUseCase,
	private val mangaListMapper: MangaListMapper,
	private val detailsLoadUseCase: DetailsLoadUseCase,
	private val progressUpdateUseCase: ProgressUpdateUseCase,
	private val readingTimeUseCase: ReadingTimeUseCase,
	private val statsRepository: StatsRepository,
) : BaseViewModel() {

	private val intent = MangaIntent(savedStateHandle)
	private var loadingJob: Job
	val mangaId = intent.mangaId

	val onActionDone = MutableEventFlow<ReversibleAction>()
	val onSelectChapter = MutableEventFlow<Long>()
	val onDownloadStarted = MutableEventFlow<Unit>()

	val details = MutableStateFlow(intent.manga?.let { MangaDetails(it, null, null, false) })
	val manga = details.map { x -> x?.toManga() }
		.withErrorHandling()
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, null)

	val history = historyRepository.observeOne(mangaId)
		.withErrorHandling()
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, null)

	val favouriteCategories = interactor.observeFavourite(mangaId)
		.withErrorHandling()
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, emptySet())

	val isStatsAvailable = statsRepository.observeHasStats(mangaId)
		.withErrorHandling()
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, false)

	val remoteManga = MutableStateFlow<Manga?>(null)

	val newChaptersCount = details.flatMapLatest { d ->
		if (d?.isLocal == false) {
			interactor.observeNewChapters(mangaId)
		} else {
			flowOf(0)
		}
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, 0)

	private val chaptersQuery = MutableStateFlow("")
	val selectedBranch = MutableStateFlow<String?>(null)

	val isChaptersReversed = settings.observeAsStateFlow(
		scope = viewModelScope + Dispatchers.Default,
		key = AppSettings.KEY_REVERSE_CHAPTERS,
		valueProducer = { isChaptersReverse },
	)

	val isChaptersInGridView = settings.observeAsStateFlow(
		scope = viewModelScope + Dispatchers.Default,
		key = AppSettings.KEY_GRID_VIEW_CHAPTERS,
		valueProducer = { isChaptersGridView },
	)

	val historyInfo: StateFlow<HistoryInfo> = combine(
		details,
		selectedBranch,
		history,
		interactor.observeIncognitoMode(manga),
	) { m, b, h, im ->
		HistoryInfo(m, b, h, im)
	}.stateIn(
		scope = viewModelScope + Dispatchers.Default,
		started = SharingStarted.Eagerly,
		initialValue = HistoryInfo(null, null, null, false),
	)

	val bookmarks = manga.flatMapLatest {
		if (it != null) bookmarksRepository.observeBookmarks(it) else flowOf(emptyList())
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Lazily, emptyList())

	val localSize = details
		.map { it?.local }
		.distinctUntilChanged()
		.combine(localStorageChanges.onStart { emit(null) }) { x, _ -> x }
		.map { local ->
			if (local != null) {
				runCatchingCancellable {
					local.file.computeSize()
				}.getOrDefault(0L)
			} else {
				0L
			}
		}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.WhileSubscribed(5000), 0L)

	val onMangaRemoved = MutableEventFlow<Manga>()
	val isScrobblingAvailable: Boolean
		get() = scrobblers.any { it.isEnabled }

	val scrobblingInfo: StateFlow<List<ScrobblingInfo>> = interactor.observeScrobblingInfo(mangaId)
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, emptyList())

	val relatedManga: StateFlow<List<MangaListModel>> = manga.mapLatest {
		if (it != null && settings.isRelatedMangaEnabled) {
			mangaListMapper.toListModelList(
				manga = relatedMangaUseCase(it).orEmpty(),
				mode = ListMode.GRID,
			)
		} else {
			emptyList()
		}
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Lazily, emptyList())

	val branches: StateFlow<List<MangaBranch>> = combine(
		details,
		selectedBranch,
		history,
	) { m, b, h ->
		val c = m?.chapters
		if (c.isNullOrEmpty()) {
			return@combine emptyList()
		}
		val currentBranch = h?.let { m.allChapters.findById(it.chapterId) }?.branch
		c.map { x ->
			MangaBranch(
				name = x.key,
				count = x.value.size,
				isSelected = x.key == b,
				isCurrent = h != null && x.key == currentBranch,
			)
		}.sortedWith(BranchComparator())
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, emptyList())

	val isChaptersEmpty: StateFlow<Boolean> = details.map {
		it != null && it.isLoaded && it.allChapters.isEmpty()
	}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

	val chapters = combine(
		combine(
			details,
			history,
			selectedBranch,
			newChaptersCount,
			bookmarks,
			isChaptersInGridView,
		) { manga, history, branch, news, bookmarks, grid ->
			manga?.mapChapters(
				history,
				news,
				branch,
				bookmarks,
				grid,
			).orEmpty()
		},
		isChaptersReversed,
		chaptersQuery,
	) { list, reversed, query ->
		(if (reversed) list.asReversed() else list).filterSearch(query)
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, emptyList())

	val readingTime = combine(
		details,
		selectedBranch,
		history,
	) { m, b, h ->
		readingTimeUseCase.invoke(m, b, h)
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Lazily, null)

	val selectedBranchValue: String?
		get() = selectedBranch.value

	init {
		loadingJob = doLoad()
		launchJob(Dispatchers.Default) {
			localStorageChanges
				.collect { onDownloadComplete(it) }
		}
		launchJob(Dispatchers.Default) {
			val manga = details.firstOrNull { !it?.chapters.isNullOrEmpty() } ?: return@launchJob
			val h = history.firstOrNull()
			if (h != null) {
				progressUpdateUseCase(manga.toManga())
			}
		}
		launchJob(Dispatchers.Default) {
			val manga = details.firstOrNull { it != null && it.isLocal } ?: return@launchJob
			remoteManga.value = interactor.findRemote(manga.toManga())
		}
	}

	fun reload() {
		loadingJob.cancel()
		loadingJob = doLoad()
	}

	fun deleteLocal() {
		val m = details.value?.local?.manga
		if (m == null) {
			errorEvent.call(FileNotFoundException())
			return
		}
		launchLoadingJob(Dispatchers.Default) {
			deleteLocalMangaUseCase(m)
			onMangaRemoved.call(m)
		}
	}

	fun removeBookmark(bookmark: Bookmark) {
		launchJob(Dispatchers.Default) {
			bookmarksRepository.removeBookmark(bookmark)
			onActionDone.call(ReversibleAction(R.string.bookmark_removed, null))
		}
	}

	fun setChaptersReversed(newValue: Boolean) {
		settings.isChaptersReverse = newValue
	}

	fun setChaptersInGridView(newValue: Boolean) {
		settings.isChaptersGridView = newValue
	}

	fun setSelectedBranch(branch: String?) {
		selectedBranch.value = branch
	}

	fun performChapterSearch(query: String?) {
		chaptersQuery.value = query?.trim().orEmpty()
	}

	fun updateScrobbling(index: Int, rating: Float, status: ScrobblingStatus?) {
		val scrobbler = getScrobbler(index) ?: return
		launchJob(Dispatchers.Default) {
			scrobbler.updateScrobblingInfo(
				mangaId = mangaId,
				rating = rating,
				status = status,
				comment = null,
			)
		}
	}

	fun unregisterScrobbling(index: Int) {
		val scrobbler = getScrobbler(index) ?: return
		launchJob(Dispatchers.Default) {
			scrobbler.unregisterScrobbling(
				mangaId = mangaId,
			)
		}
	}

	fun markChapterAsCurrent(chapterId: Long) {
		launchJob(Dispatchers.Default) {
			val manga = checkNotNull(details.value)
			val chapters = checkNotNull(manga.chapters[selectedBranchValue])
			val chapterIndex = chapters.indexOfFirst { it.id == chapterId }
			check(chapterIndex in chapters.indices) { "Chapter not found" }
			val percent = chapterIndex / chapters.size.toFloat()
			historyRepository.addOrUpdate(
				manga = manga.toManga(),
				chapterId = chapterId,
				page = 0,
				scroll = 0,
				percent = percent,
				force = true,
			)
		}
	}

	fun download(chaptersIds: Set<Long>?) {
		launchJob(Dispatchers.Default) {
			downloadScheduler.schedule(
				details.requireValue().toManga(),
				chaptersIds,
			)
			onDownloadStarted.call(Unit)
		}
	}

	fun startChaptersSelection() {
		val chapters = chapters.value
		val chapter = chapters.find {
			it.isUnread && !it.isDownloaded
		} ?: chapters.firstOrNull() ?: return
		onSelectChapter.call(chapter.chapter.id)
	}

	fun removeFromHistory() {
		launchJob(Dispatchers.Default) {
			val handle = historyRepository.delete(setOf(mangaId))
			onActionDone.call(ReversibleAction(R.string.removed_from_history, handle))
		}
	}

	private fun doLoad() = launchLoadingJob(Dispatchers.Default) {
		detailsLoadUseCase.invoke(intent)
			.onEachWhile {
				if (it.allChapters.isEmpty()) {
					return@onEachWhile false
				}
				val manga = it.toManga()
				// find default branch
				val hist = historyRepository.getOne(manga)
				selectedBranch.value = manga.getPreferredBranch(hist)
				true
			}.collect {
				details.value = it
			}
	}

	private fun List<ChapterListItem>.filterSearch(query: String): List<ChapterListItem> {
		if (query.isEmpty() || this.isEmpty()) {
			return this
		}
		return filter {
			it.chapter.name.contains(query, ignoreCase = true)
		}
	}

	private suspend fun onDownloadComplete(downloadedManga: LocalManga?) {
		downloadedManga ?: return
		launchJob {
			details.update {
				interactor.updateLocal(it, downloadedManga)
			}
		}
	}

	private fun getScrobbler(index: Int): Scrobbler? {
		val info = scrobblingInfo.value.getOrNull(index)
		val scrobbler = if (info != null) {
			scrobblers.find { it.scrobblerService == info.scrobbler && it.isEnabled }
		} else {
			null
		}
		if (scrobbler == null) {
			errorEvent.call(IllegalStateException("Scrobbler [$index] is not available"))
		}
		return scrobbler
	}
}
