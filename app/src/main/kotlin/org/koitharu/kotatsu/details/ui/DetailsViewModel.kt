package org.koitharu.kotatsu.details.ui

import android.text.Html
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.core.net.toUri
import androidx.core.text.getSpans
import androidx.core.text.parseAsHtml
import androidx.lifecycle.LiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.bookmarks.domain.Bookmark
import org.koitharu.kotatsu.bookmarks.domain.BookmarksRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.observeAsFlow
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.util.SingleLiveEvent
import org.koitharu.kotatsu.core.util.asFlowLiveData
import org.koitharu.kotatsu.core.util.ext.computeSize
import org.koitharu.kotatsu.core.util.ext.toFileOrNull
import org.koitharu.kotatsu.details.domain.BranchComparator
import org.koitharu.kotatsu.details.ui.model.ChapterListItem
import org.koitharu.kotatsu.details.ui.model.HistoryInfo
import org.koitharu.kotatsu.details.ui.model.MangaBranch
import org.koitharu.kotatsu.download.ui.worker.DownloadWorker
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository
import org.koitharu.kotatsu.history.domain.HistoryRepository
import org.koitharu.kotatsu.local.data.LocalManga
import org.koitharu.kotatsu.local.data.LocalStorageChanges
import org.koitharu.kotatsu.local.domain.LocalMangaRepository
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import org.koitharu.kotatsu.scrobbling.common.domain.Scrobbler
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblingInfo
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblingStatus
import org.koitharu.kotatsu.tracker.domain.TrackingRepository
import org.koitharu.kotatsu.util.ext.printStackTraceDebug
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class DetailsViewModel @Inject constructor(
	private val historyRepository: HistoryRepository,
	favouritesRepository: FavouritesRepository,
	private val localMangaRepository: LocalMangaRepository,
	trackingRepository: TrackingRepository,
	private val bookmarksRepository: BookmarksRepository,
	private val settings: AppSettings,
	private val scrobblers: Set<@JvmSuppressWildcards Scrobbler>,
	private val imageGetter: Html.ImageGetter,
	private val delegate: MangaDetailsDelegate,
	@LocalStorageChanges private val localStorageChanges: SharedFlow<LocalManga?>,
	private val downloadScheduler: DownloadWorker.Scheduler,
) : BaseViewModel() {

	private var loadingJob: Job

	val onShowToast = SingleLiveEvent<Int>()
	val onDownloadStarted = SingleLiveEvent<Unit>()

	private val mangaData = combine(
		delegate.onlineManga,
		delegate.localManga,
	) { o, l ->
		o ?: l
	}.stateIn(viewModelScope, SharingStarted.Lazily, null)

	private val history = historyRepository.observeOne(delegate.mangaId)
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, null)

	private val favourite = favouritesRepository.observeCategoriesIds(delegate.mangaId).map { it.isNotEmpty() }
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, false)

	private val newChapters = settings.observeAsFlow(AppSettings.KEY_TRACKER_ENABLED) { isTrackerEnabled }
		.flatMapLatest { isEnabled ->
			if (isEnabled) {
				trackingRepository.observeNewChaptersCount(delegate.mangaId)
			} else {
				flowOf(0)
			}
		}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, 0)

	private val chaptersQuery = MutableStateFlow("")

	private val chaptersReversed = settings.observeAsFlow(AppSettings.KEY_REVERSE_CHAPTERS) { chaptersReverse }
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, false)

	val manga = mangaData.filterNotNull().asLiveData(viewModelScope.coroutineContext)
	val favouriteCategories = favourite.asLiveData(viewModelScope.coroutineContext)
	val newChaptersCount = newChapters.asLiveData(viewModelScope.coroutineContext)
	val isChaptersReversed = chaptersReversed.asLiveData(viewModelScope.coroutineContext)

	val historyInfo: LiveData<HistoryInfo> = combine(
		mangaData,
		delegate.selectedBranch,
		history,
		historyRepository.observeShouldSkip(mangaData),
	) { m, b, h, im ->
		HistoryInfo(m, b, h, im)
	}.asFlowLiveData(
		context = viewModelScope.coroutineContext + Dispatchers.Default,
		defaultValue = HistoryInfo(null, null, null, false),
	)

	val bookmarks = mangaData.flatMapLatest {
		if (it != null) bookmarksRepository.observeBookmarks(it) else flowOf(emptyList())
	}.asFlowLiveData(viewModelScope.coroutineContext + Dispatchers.Default, emptyList())

	val localSize = delegate.localManga
		.map {
			if (it != null) {
				val file = it.url.toUri().toFileOrNull()
				file?.computeSize() ?: 0L
			} else {
				0L
			}
		}.asFlowLiveData(viewModelScope.coroutineContext + Dispatchers.Default, 0)

	val description = mangaData
		.distinctUntilChangedBy { it?.description.orEmpty() }
		.transformLatest {
			val description = it?.description
			if (description.isNullOrEmpty()) {
				emit(null)
			} else {
				emit(description.parseAsHtml().filterSpans())
				emit(description.parseAsHtml(imageGetter = imageGetter).filterSpans())
			}
		}.asFlowLiveData(viewModelScope.coroutineContext + Dispatchers.Default, null)

	val onMangaRemoved = SingleLiveEvent<Manga>()
	val isScrobblingAvailable: Boolean
		get() = scrobblers.any { it.isAvailable }

	val scrobblingInfo: LiveData<List<ScrobblingInfo>> = combine(
		scrobblers.map { it.observeScrobblingInfo(delegate.mangaId) },
	) { scrobblingInfo ->
		scrobblingInfo.filterNotNull()
	}.asFlowLiveData(viewModelScope.coroutineContext + Dispatchers.Default, emptyList())

	val branches: LiveData<List<MangaBranch>> = combine(
		delegate.onlineManga,
		delegate.localManga,
		delegate.selectedBranch,
	) { m, l, b ->
		val chapters = concat(m?.chapters, l?.chapters)
		if (chapters.isEmpty()) return@combine emptyList()
		chapters.groupBy { x -> x.branch }
			.map { x -> MangaBranch(x.key, x.value.size, x.key == b) }
			.sortedWith(BranchComparator())
	}.asFlowLiveData(viewModelScope.coroutineContext + Dispatchers.Default, emptyList())

	val selectedBranchName = delegate.selectedBranch
		.asFlowLiveData(viewModelScope.coroutineContext, null)

	val isChaptersEmpty: LiveData<Boolean> = combine(
		delegate.onlineManga,
		delegate.localManga,
		isLoading.asFlow(),
	) { manga, local, loading ->
		(manga != null && manga.chapters.isNullOrEmpty()) &&
			(local != null && local.chapters.isNullOrEmpty()) &&
			!loading
	}.asFlowLiveData(viewModelScope.coroutineContext, false)

	val chapters = combine(
		combine(
			delegate.onlineManga,
			delegate.localManga,
			history,
			delegate.selectedBranch,
			newChapters,
		) { manga, local, history, branch, news ->
			mapChapters(manga, local, history, news, branch)
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
		launchJob(Dispatchers.Default) {
			localStorageChanges
				.collect { onDownloadComplete(it) }
		}
	}

	fun reload() {
		loadingJob.cancel()
		loadingJob = doLoad()
	}

	fun deleteLocal() {
		val m = delegate.localManga.value
		if (m == null) {
			onShowToast.call(R.string.file_not_found)
			return
		}
		launchLoadingJob(Dispatchers.Default) {
			val manga = if (m.source == MangaSource.LOCAL) m else localMangaRepository.findSavedManga(m)?.manga
			checkNotNull(manga) { "Cannot find saved manga for ${m.title}" }
			val original = localMangaRepository.getRemoteManga(manga)
			localMangaRepository.delete(manga) || throw IOException("Unable to delete file")
			runCatchingCancellable {
				historyRepository.deleteOrSwap(manga, original)
			}
			onMangaRemoved.emitCall(manga)
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
		return delegate.onlineManga.value
	}

	fun performChapterSearch(query: String?) {
		chaptersQuery.value = query?.trim().orEmpty()
	}

	fun updateScrobbling(index: Int, rating: Float, status: ScrobblingStatus?) {
		val scrobbler = getScrobbler(index) ?: return
		launchJob(Dispatchers.Default) {
			scrobbler.updateScrobblingInfo(
				mangaId = delegate.mangaId,
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
				mangaId = delegate.mangaId,
			)
		}
	}

	fun markChapterAsCurrent(chapterId: Long) {
		launchJob(Dispatchers.Default) {
			val manga = checkNotNull(mangaData.value)
			val chapters = checkNotNull(manga.getChapters(selectedBranchValue))
			val chapterIndex = chapters.indexOfFirst { it.id == chapterId }
			check(chapterIndex in chapters.indices) { "Chapter not found" }
			val percent = chapterIndex / chapters.size.toFloat()
			historyRepository.addOrUpdate(manga = manga, chapterId = chapterId, page = 0, scroll = 0, percent = percent)
		}
	}

	fun download(chaptersIds: Set<Long>?) {
		launchJob(Dispatchers.Default) {
			downloadScheduler.schedule(
				delegate.onlineManga.value ?: checkNotNull(manga.value),
				chaptersIds,
			)
			onDownloadStarted.emitCall(Unit)
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

	private suspend fun onDownloadComplete(downloadedManga: LocalManga?) {
		downloadedManga ?: return
		val currentManga = mangaData.value ?: return
		if (currentManga.id != downloadedManga.manga.id) {
			return
		}
		if (currentManga.source == MangaSource.LOCAL) {
			reload()
		} else {
			viewModelScope.launch(Dispatchers.Default) {
				runCatchingCancellable {
					localMangaRepository.getDetails(downloadedManga.manga)
				}.onSuccess {
					delegate.publishManga(it)
				}.onFailure {
					it.printStackTraceDebug()
				}
			}
		}
	}

	private fun Spanned.filterSpans(): CharSequence {
		val spannable = SpannableString.valueOf(this)
		val spans = spannable.getSpans<ForegroundColorSpan>()
		for (span in spans) {
			spannable.removeSpan(span)
		}
		return spannable.trim()
	}

	private fun getScrobbler(index: Int): Scrobbler? {
		val info = scrobblingInfo.value?.getOrNull(index)
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

	private fun <T> concat(a: List<T>?, b: List<T>?): List<T> {
		return when {
			a == null && b == null -> emptyList<T>()
			a == null && b != null -> b
			a != null && b == null -> a
			a != null && b != null -> buildList<T>(a.size + b.size) {
				addAll(a)
				addAll(b)
			}

			else -> error("This shouldn't have happened")
		}
	}
}
