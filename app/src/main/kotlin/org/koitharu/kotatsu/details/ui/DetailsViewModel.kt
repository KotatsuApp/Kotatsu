package org.koitharu.kotatsu.details.ui

import android.text.Html
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.core.net.toUri
import androidx.core.text.getSpans
import androidx.core.text.parseAsHtml
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
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.bookmarks.domain.Bookmark
import org.koitharu.kotatsu.bookmarks.domain.BookmarksRepository
import org.koitharu.kotatsu.core.model.getPreferredBranch
import org.koitharu.kotatsu.core.os.NetworkState
import org.koitharu.kotatsu.core.parser.MangaIntent
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.core.prefs.observeAsStateFlow
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.util.ext.MutableEventFlow
import org.koitharu.kotatsu.core.util.ext.call
import org.koitharu.kotatsu.core.util.ext.combine
import org.koitharu.kotatsu.core.util.ext.computeSize
import org.koitharu.kotatsu.core.util.ext.onFirst
import org.koitharu.kotatsu.core.util.ext.requireValue
import org.koitharu.kotatsu.core.util.ext.sanitize
import org.koitharu.kotatsu.core.util.ext.toFileOrNull
import org.koitharu.kotatsu.details.domain.BranchComparator
import org.koitharu.kotatsu.details.domain.DetailsInteractor
import org.koitharu.kotatsu.details.domain.DoubleMangaLoadUseCase
import org.koitharu.kotatsu.details.domain.RelatedMangaUseCase
import org.koitharu.kotatsu.details.domain.model.DoubleManga
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
	private val imageGetter: Html.ImageGetter,
	@LocalStorageChanges private val localStorageChanges: SharedFlow<LocalManga?>,
	private val downloadScheduler: DownloadWorker.Scheduler,
	private val interactor: DetailsInteractor,
	savedStateHandle: SavedStateHandle,
	private val deleteLocalMangaUseCase: DeleteLocalMangaUseCase,
	private val doubleMangaLoadUseCase: DoubleMangaLoadUseCase,
	private val relatedMangaUseCase: RelatedMangaUseCase,
	private val extraProvider: ListExtraProvider,
	networkState: NetworkState,
) : BaseViewModel() {

	private val intent = MangaIntent(savedStateHandle)
	private val mangaId = intent.mangaId
	private val doubleManga: MutableStateFlow<DoubleManga?> =
		MutableStateFlow(intent.manga?.let { DoubleManga(it) })
	private var loadingJob: Job

	val onShowToast = MutableEventFlow<Int>()
	val onShowTip = MutableEventFlow<Unit>()
	val onSelectChapter = MutableEventFlow<Long>()
	val onDownloadStarted = MutableEventFlow<Unit>()

	val manga = doubleManga.map { it?.any }
		.stateIn(viewModelScope, SharingStarted.Eagerly, doubleManga.value?.any)

	val history = historyRepository.observeOne(mangaId)
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, null)

	val favouriteCategories = interactor.observeIsFavourite(mangaId)
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, false)

	val newChaptersCount = interactor.observeNewChapters(mangaId)
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, 0)

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

	val localSize = doubleManga
		.map {
			val local = it?.local
			if (local != null) {
				val file = local.url.toUri().toFileOrNull()
				file?.computeSize() ?: 0L
			} else {
				0L
			}
		}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.WhileSubscribed(), 0)

	val description = manga
		.distinctUntilChangedBy { it?.description.orEmpty() }
		.transformLatest {
			val description = it?.description
			if (description.isNullOrEmpty()) {
				emit(null)
			} else {
				emit(description.parseAsHtml().filterSpans().sanitize())
				emit(description.parseAsHtml(imageGetter = imageGetter).filterSpans())
			}
		}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.WhileSubscribed(5000), null)

	val onMangaRemoved = MutableEventFlow<Manga>()
	val isScrobblingAvailable: Boolean
		get() = scrobblers.any { it.isAvailable }

	val scrobblingInfo: StateFlow<List<ScrobblingInfo>> = interactor.observeScrobblingInfo(mangaId)
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, emptyList())

	val relatedManga: StateFlow<List<MangaItemModel>> = doubleManga.map {
		it?.remote
	}.distinctUntilChangedBy { it?.id }
		.mapLatest {
			if (it != null && settings.isRelatedMangaEnabled) {
				relatedMangaUseCase.invoke(it)?.toUi(ListMode.GRID, extraProvider).orEmpty()
			} else {
				emptyList()
			}
		}
		.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

	val branches: StateFlow<List<MangaBranch>> = combine(
		doubleManga,
		selectedBranch,
	) { m, b ->
		val chapters = m?.chapters
		if (chapters.isNullOrEmpty()) return@combine emptyList()
		chapters.groupBy { x -> x.branch }
			.map { x -> MangaBranch(x.key, x.value.size, x.key == b) }
			.sortedWith(BranchComparator())
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, emptyList())

	val isChaptersEmpty: StateFlow<Boolean> = combine(
		doubleManga,
		isLoading,
	) { manga, loading ->
		manga?.any != null && manga.chapters.isNullOrEmpty() && !loading
	}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

	val chapters = combine(
		combine(
			doubleManga,
			history,
			selectedBranch,
			newChaptersCount,
			bookmarks,
			networkState,
		) { manga, history, branch, news, bookmarks, isOnline ->
			mapChapters(
				manga?.remote?.takeIf { isOnline },
				manga?.local,
				history,
				news,
				branch,
				bookmarks,
			)
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
	}

	fun reload() {
		loadingJob.cancel()
		loadingJob = doLoad()
	}

	fun deleteLocal() {
		val m = doubleManga.value?.local
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
			val manga = checkNotNull(doubleManga.value)
			val chapters = checkNotNull(manga.filterChapters(selectedBranchValue).chapters)
			val chapterIndex = chapters.indexOfFirst { it.id == chapterId }
			check(chapterIndex in chapters.indices) { "Chapter not found" }
			val percent = chapterIndex / chapters.size.toFloat()
			historyRepository.addOrUpdate(
				manga = manga.requireAny(),
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
				doubleManga.requireValue().requireAny(),
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
		doubleMangaLoadUseCase.invoke(intent)
			.onFirst {
				val manga = it.requireAny()
				// find default branch
				val hist = historyRepository.getOne(manga)
				selectedBranch.value = manga.getPreferredBranch(hist)
			}.collect {
				doubleManga.value = it
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
			doubleManga.update {
				interactor.updateLocal(it, downloadedManga)
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
