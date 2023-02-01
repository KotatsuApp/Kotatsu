package org.koitharu.kotatsu.details.ui

import android.text.Html
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.core.text.getSpans
import androidx.core.text.parseAsHtml
import androidx.lifecycle.LiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
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
import org.koitharu.kotatsu.base.domain.MangaDataRepository
import org.koitharu.kotatsu.base.domain.MangaIntent
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.bookmarks.domain.Bookmark
import org.koitharu.kotatsu.bookmarks.domain.BookmarksRepository
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.observeAsFlow
import org.koitharu.kotatsu.details.domain.BranchComparator
import org.koitharu.kotatsu.details.ui.model.ChapterListItem
import org.koitharu.kotatsu.details.ui.model.HistoryInfo
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository
import org.koitharu.kotatsu.history.domain.HistoryRepository
import org.koitharu.kotatsu.local.domain.LocalMangaRepository
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.util.mapToSet
import org.koitharu.kotatsu.scrobbling.domain.Scrobbler
import org.koitharu.kotatsu.scrobbling.domain.model.ScrobblingInfo
import org.koitharu.kotatsu.scrobbling.domain.model.ScrobblingStatus
import org.koitharu.kotatsu.tracker.domain.TrackingRepository
import org.koitharu.kotatsu.utils.SingleLiveEvent
import org.koitharu.kotatsu.utils.asFlowLiveData
import org.koitharu.kotatsu.utils.ext.asLiveDataDistinct
import org.koitharu.kotatsu.utils.ext.printStackTraceDebug
import org.koitharu.kotatsu.utils.ext.runCatchingCancellable
import java.io.IOException

class DetailsViewModel @AssistedInject constructor(
	@Assisted intent: MangaIntent,
	private val historyRepository: HistoryRepository,
	favouritesRepository: FavouritesRepository,
	private val localMangaRepository: LocalMangaRepository,
	trackingRepository: TrackingRepository,
	mangaDataRepository: MangaDataRepository,
	private val bookmarksRepository: BookmarksRepository,
	private val settings: AppSettings,
	private val scrobblers: Set<@JvmSuppressWildcards Scrobbler>,
	private val imageGetter: Html.ImageGetter,
	mangaRepositoryFactory: MangaRepository.Factory,
) : BaseViewModel() {

	private val delegate = MangaDetailsDelegate(
		intent = intent,
		settings = settings,
		mangaDataRepository = mangaDataRepository,
		historyRepository = historyRepository,
		localMangaRepository = localMangaRepository,
		mangaRepositoryFactory = mangaRepositoryFactory,
	)

	private var loadingJob: Job

	val onShowToast = SingleLiveEvent<Int>()

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

	val manga = delegate.manga.filterNotNull().asLiveData(viewModelScope.coroutineContext)
	val favouriteCategories = favourite.asLiveData(viewModelScope.coroutineContext)
	val newChaptersCount = newChapters.asLiveData(viewModelScope.coroutineContext)
	val isChaptersReversed = chaptersReversed.asLiveData(viewModelScope.coroutineContext)

	val historyInfo: LiveData<HistoryInfo> = combine(
		delegate.manga,
		history,
		historyRepository.observeShouldSkip(delegate.manga),
	) { m, h, im ->
		HistoryInfo(m, h, im)
	}.asFlowLiveData(
		context = viewModelScope.coroutineContext + Dispatchers.Default,
		defaultValue = HistoryInfo(null, null, false),
	)

	val bookmarks = delegate.manga.flatMapLatest {
		if (it != null) bookmarksRepository.observeBookmarks(it) else flowOf(emptyList())
	}.asLiveDataDistinct(viewModelScope.coroutineContext + Dispatchers.Default, emptyList())

	val description = delegate.manga
		.distinctUntilChangedBy { it?.description.orEmpty() }
		.transformLatest {
			val description = it?.description
			if (description.isNullOrEmpty()) {
				emit(null)
			} else {
				emit(description.parseAsHtml().filterSpans())
				emit(description.parseAsHtml(imageGetter = imageGetter).filterSpans())
			}
		}.asLiveDataDistinct(viewModelScope.coroutineContext + Dispatchers.Default, null)

	val onMangaRemoved = SingleLiveEvent<Manga>()
	val isScrobblingAvailable: Boolean
		get() = scrobblers.any { it.isAvailable }

	val scrobblingInfo: LiveData<List<ScrobblingInfo>> = combine(
		scrobblers.map { it.observeScrobblingInfo(delegate.mangaId) },
	) { scrobblingInfo ->
		scrobblingInfo.filterNotNull()
	}.asFlowLiveData(viewModelScope.coroutineContext + Dispatchers.Default, emptyList())

	val branches: LiveData<List<String?>> = delegate.manga.map {
		val chapters = it?.chapters ?: return@map emptyList()
		chapters.mapToSet { x -> x.branch }.sortedWith(BranchComparator())
	}.asFlowLiveData(viewModelScope.coroutineContext + Dispatchers.Default, emptyList())

	val selectedBranchIndex = combine(
		branches.asFlow(),
		delegate.selectedBranch,
	) { branches, selected ->
		branches.indexOf(selected)
	}.asLiveDataDistinct(viewModelScope.coroutineContext + Dispatchers.Default, -1)

	val selectedBranchName = delegate.selectedBranch
		.asFlowLiveData(viewModelScope.coroutineContext, null)

	val isChaptersEmpty: LiveData<Boolean> = combine(
		delegate.manga,
		isLoading.asFlow(),
	) { m, loading ->
		m != null && m.chapters.isNullOrEmpty() && !loading
	}.asLiveDataDistinct(viewModelScope.coroutineContext, false)

	val chapters = combine(
		combine(
			delegate.manga,
			delegate.relatedManga,
			history,
			delegate.selectedBranch,
			newChapters,
		) { manga, related, history, branch, news ->
			delegate.mapChapters(manga, related, history, news, branch)
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
			runCatchingCancellable {
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
				runCatchingCancellable {
					localMangaRepository.getDetails(downloadedManga)
				}.onSuccess {
					delegate.relatedManga.value = it
				}.onFailure {
					it.printStackTraceDebug()
				}
			}
		}
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
			val manga = checkNotNull(delegate.manga.value)
			val chapters = checkNotNull(manga.chapters)
			val chapterIndex = chapters.indexOfFirst { it.id == chapterId }
			check(chapterIndex in chapters.indices) { "Chapter not found" }
			val percent = chapterIndex / chapters.size.toFloat()
			historyRepository.addOrUpdate(manga = manga, chapterId = chapterId, page = 0, scroll = 0, percent = percent)
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

	@AssistedFactory
	interface Factory {

		fun create(intent: MangaIntent): DetailsViewModel
	}
}
