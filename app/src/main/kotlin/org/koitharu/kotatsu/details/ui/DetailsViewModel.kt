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
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.bookmarks.domain.Bookmark
import org.koitharu.kotatsu.bookmarks.domain.BookmarksRepository
import org.koitharu.kotatsu.core.model.getPreferredBranch
import org.koitharu.kotatsu.core.parser.MangaIntent
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.core.prefs.observeAsStateFlow
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.util.ext.MutableEventFlow
import org.koitharu.kotatsu.core.util.ext.call
import org.koitharu.kotatsu.core.util.ext.computeSize
import org.koitharu.kotatsu.core.util.ext.onEachWhile
import org.koitharu.kotatsu.core.util.ext.requireValue
import org.koitharu.kotatsu.details.data.MangaDetails
import org.koitharu.kotatsu.details.domain.BranchComparator
import org.koitharu.kotatsu.details.domain.DetailsInteractor
import org.koitharu.kotatsu.details.domain.DetailsLoadUseCase
import org.koitharu.kotatsu.details.domain.ProgressUpdateUseCase
import org.koitharu.kotatsu.details.domain.RelatedMangaUseCase
import org.koitharu.kotatsu.details.ui.model.ChapterListItem
import org.koitharu.kotatsu.details.ui.model.HistoryInfo
import org.koitharu.kotatsu.details.ui.model.MangaBranch
import org.koitharu.kotatsu.download.ui.worker.DownloadWorker
import org.koitharu.kotatsu.history.data.HistoryRepository
import org.koitharu.kotatsu.list.domain.ListExtraProvider
import org.koitharu.kotatsu.list.ui.model.MangaItemModel
import org.koitharu.kotatsu.list.ui.model.toUi
import org.koitharu.kotatsu.local.data.LocalStorageChanges
import org.koitharu.kotatsu.local.domain.DeleteLocalMangaUseCase
import org.koitharu.kotatsu.local.domain.model.LocalManga
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import org.koitharu.kotatsu.scrobbling.common.domain.Scrobbler
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblingInfo
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblingStatus
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
	private val extraProvider: ListExtraProvider,
	private val detailsLoadUseCase: DetailsLoadUseCase,
	private val progressUpdateUseCase: ProgressUpdateUseCase,
) : BaseViewModel() {

	private val intent = MangaIntent(savedStateHandle)
	private val mangaId = intent.mangaId
	private var loadingJob: Job

	val onShowToast = MutableEventFlow<Int>()
	val onShowTip = MutableEventFlow<Unit>()
	val onSelectChapter = MutableEventFlow<Long>()
	val onDownloadStarted = MutableEventFlow<Unit>()

	val details = MutableStateFlow(intent.manga?.let { MangaDetails(it, null, null, false) })
	val manga = details.map { x -> x?.toManga() }
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, null)

	val history = historyRepository.observeOne(mangaId)
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, null)

	val favouriteCategories = interactor.observeIsFavourite(mangaId)
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
		valueProducer = { chaptersReverse },
	)

	val historyInfo: StateFlow<HistoryInfo> = combine(
		manga,
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
		.map { local ->
			if (local != null) {
				runCatchingCancellable {
					local.file.computeSize()
				}.getOrDefault(0L)
			} else {
				0L
			}
		}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.WhileSubscribed(5000), 0L)

	@Deprecated("")
	val description = details
		.map { it?.description }
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Lazily, null)

	val onMangaRemoved = MutableEventFlow<Manga>()
	val isScrobblingAvailable: Boolean
		get() = scrobblers.any { it.isAvailable }

	val scrobblingInfo: StateFlow<List<ScrobblingInfo>> = interactor.observeScrobblingInfo(mangaId)
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, emptyList())

	val relatedManga: StateFlow<List<MangaItemModel>> = manga.mapLatest {
		if (it != null && settings.isRelatedMangaEnabled) {
			relatedMangaUseCase.invoke(it)?.toUi(ListMode.GRID, extraProvider).orEmpty()
		} else {
			emptyList()
		}
	}.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

	val branches: StateFlow<List<MangaBranch>> = combine(
		details,
		selectedBranch,
	) { m, b ->
		(m?.chapters ?: return@combine emptyList())
			.map { x -> MangaBranch(x.key, x.value.size, x.key == b) }
			.sortedWith(BranchComparator())
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
		) { manga, history, branch, news, bookmarks ->
			manga?.mapChapters(
				history,
				news,
				branch,
				bookmarks,
			).orEmpty()
		},
		isChaptersReversed,
		chaptersQuery,
	) { list, reversed, query ->
		(if (reversed) list.asReversed() else list).filterSearch(query)
	}.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

	val selectedBranchValue: String?
		get() = selectedBranch.value

	init {
		loadingJob = doLoad()
		launchJob(Dispatchers.Default) {
			localStorageChanges
				.collect { onDownloadComplete(it) }
		}
		launchJob(Dispatchers.Default) {
			if (settings.isTipEnabled(DetailsActivity.TIP_BUTTON)) {
				manga.filterNot { it?.chapters.isNullOrEmpty() }.first()
				onShowTip.call(Unit)
			}
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
			onShowToast.call(R.string.file_not_found)
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
			onShowToast.call(R.string.bookmark_removed)
		}
	}

	fun setChaptersReversed(newValue: Boolean) {
		settings.chaptersReverse = newValue
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

	fun onButtonTipClosed() {
		settings.closeTip(DetailsActivity.TIP_BUTTON)
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
			scrobblers.find { it.scrobblerService == info.scrobbler && it.isAvailable }
		} else {
			null
		}
		if (scrobbler == null) {
			errorEvent.call(IllegalStateException("Scrobbler [$index] is not available"))
		}
		return scrobbler
	}
}
