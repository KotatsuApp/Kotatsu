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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.bookmarks.domain.BookmarksRepository
import org.koitharu.kotatsu.core.model.getPreferredBranch
import org.koitharu.kotatsu.core.nav.MangaIntent
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.core.prefs.TriStateOption
import org.koitharu.kotatsu.core.ui.util.ReversibleAction
import org.koitharu.kotatsu.core.util.ext.call
import org.koitharu.kotatsu.core.util.ext.computeSize
import org.koitharu.kotatsu.core.util.ext.onEachWhile
import org.koitharu.kotatsu.details.data.MangaDetails
import org.koitharu.kotatsu.details.domain.BranchComparator
import org.koitharu.kotatsu.details.domain.DetailsInteractor
import org.koitharu.kotatsu.details.domain.DetailsLoadUseCase
import org.koitharu.kotatsu.details.domain.ProgressUpdateUseCase
import org.koitharu.kotatsu.details.domain.ReadingTimeUseCase
import org.koitharu.kotatsu.details.domain.RelatedMangaUseCase
import org.koitharu.kotatsu.details.ui.model.HistoryInfo
import org.koitharu.kotatsu.details.ui.model.MangaBranch
import org.koitharu.kotatsu.details.ui.pager.ChaptersPagesViewModel
import org.koitharu.kotatsu.download.ui.worker.DownloadWorker
import org.koitharu.kotatsu.history.data.HistoryRepository
import org.koitharu.kotatsu.list.domain.MangaListMapper
import org.koitharu.kotatsu.list.ui.model.MangaListModel
import org.koitharu.kotatsu.local.data.LocalStorageChanges
import org.koitharu.kotatsu.local.domain.DeleteLocalMangaUseCase
import org.koitharu.kotatsu.local.domain.model.LocalManga
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.util.findById
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import org.koitharu.kotatsu.reader.ui.ReaderState
import org.koitharu.kotatsu.scrobbling.common.domain.Scrobbler
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblingInfo
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblingStatus
import org.koitharu.kotatsu.stats.data.StatsRepository
import javax.inject.Inject

@HiltViewModel
class DetailsViewModel @Inject constructor(
	private val historyRepository: HistoryRepository,
	bookmarksRepository: BookmarksRepository,
	settings: AppSettings,
	private val scrobblers: Set<@JvmSuppressWildcards Scrobbler>,
	@LocalStorageChanges localStorageChanges: SharedFlow<LocalManga?>,
	downloadScheduler: DownloadWorker.Scheduler,
	interactor: DetailsInteractor,
	savedStateHandle: SavedStateHandle,
	deleteLocalMangaUseCase: DeleteLocalMangaUseCase,
	private val relatedMangaUseCase: RelatedMangaUseCase,
	private val mangaListMapper: MangaListMapper,
	private val detailsLoadUseCase: DetailsLoadUseCase,
	private val progressUpdateUseCase: ProgressUpdateUseCase,
	private val readingTimeUseCase: ReadingTimeUseCase,
	statsRepository: StatsRepository,
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
	private var loadingJob: Job
	val mangaId = intent.mangaId

	init {
		mangaDetails.value = intent.manga?.let { MangaDetails(it) }
	}

	val history = historyRepository.observeOne(mangaId)
		.onEach { h ->
			readingState.value = h?.let(::ReaderState)
		}.withErrorHandling()
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, null)

	val favouriteCategories = interactor.observeFavourite(mangaId)
		.withErrorHandling()
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, emptySet())

	val isStatsAvailable = statsRepository.observeHasStats(mangaId)
		.withErrorHandling()
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, false)

	val remoteManga = MutableStateFlow<Manga?>(null)

	val historyInfo: StateFlow<HistoryInfo> = combine(
		mangaDetails,
		selectedBranch,
		history,
		interactor.observeIncognitoMode(manga),
	) { m, b, h, im ->
		val estimatedTime = readingTimeUseCase.invoke(m, b, h)
		HistoryInfo(m, b, h, im == TriStateOption.ENABLED, estimatedTime)
	}.withErrorHandling()
		.stateIn(
			scope = viewModelScope + Dispatchers.Default,
			started = SharingStarted.Eagerly,
			initialValue = HistoryInfo(null, null, null, false, null),
		)

	val localSize = mangaDetails
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

	val isScrobblingAvailable: Boolean
		get() = scrobblers.any { it.isEnabled }

	val scrobblingInfo: StateFlow<List<ScrobblingInfo>> = interactor.observeScrobblingInfo(mangaId)
		.withErrorHandling()
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

	val tags = manga.mapLatest {
		mangaListMapper.mapTags(it?.tags.orEmpty())
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, emptyList())

	val branches: StateFlow<List<MangaBranch>> = combine(
		mangaDetails,
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

	val selectedBranchValue: String?
		get() = selectedBranch.value

	init {
		loadingJob = doLoad(force = false)
		launchJob(Dispatchers.Default + SkipErrors) {
			val manga = mangaDetails.firstOrNull { !it?.chapters.isNullOrEmpty() } ?: return@launchJob
			val h = history.firstOrNull()
			if (h != null) {
				progressUpdateUseCase(manga.toManga())
			}
		}
		launchJob(Dispatchers.Default) {
			val manga = mangaDetails.firstOrNull { it != null && it.isLocal } ?: return@launchJob
			remoteManga.value = interactor.findRemote(manga.toManga())
		}
	}

	fun reload() {
		loadingJob.cancel()
		loadingJob = doLoad(force = true)
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

	fun removeFromHistory() {
		launchJob(Dispatchers.Default) {
			val handle = historyRepository.delete(setOf(mangaId))
			onActionDone.call(ReversibleAction(R.string.removed_from_history, handle))
		}
	}

	private fun doLoad(force: Boolean) = launchLoadingJob(Dispatchers.Default) {
		detailsLoadUseCase.invoke(intent, force)
			.onEachWhile {
				if (it.allChapters.isNotEmpty()) {
					val manga = it.toManga()
					// find default branch
					val hist = historyRepository.getOne(manga)
					selectedBranch.value = manga.getPreferredBranch(hist)
					true
				} else {
					false
				}
			}.collect {
				mangaDetails.value = it
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
