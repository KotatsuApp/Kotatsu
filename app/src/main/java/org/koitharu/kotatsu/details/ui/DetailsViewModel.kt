package org.koitharu.kotatsu.details.ui

import androidx.core.os.LocaleListCompat
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.base.domain.MangaDataRepository
import org.koitharu.kotatsu.base.domain.MangaIntent
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.exceptions.MangaNotFoundException
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.details.ui.model.ChapterListItem
import org.koitharu.kotatsu.details.ui.model.toListItem
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository
import org.koitharu.kotatsu.history.domain.HistoryRepository
import org.koitharu.kotatsu.local.domain.LocalMangaRepository
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.util.toTitleCase
import org.koitharu.kotatsu.tracker.domain.TrackingRepository
import org.koitharu.kotatsu.utils.SingleLiveEvent
import org.koitharu.kotatsu.utils.ext.iterator
import org.koitharu.kotatsu.utils.ext.mapToSet
import java.io.IOException

class DetailsViewModel(
	private val intent: MangaIntent,
	private val historyRepository: HistoryRepository,
	private val favouritesRepository: FavouritesRepository,
	private val localMangaRepository: LocalMangaRepository,
	private val trackingRepository: TrackingRepository,
	private val mangaDataRepository: MangaDataRepository,
	private val settings: AppSettings,
) : BaseViewModel() {

	private var loadingJob: Job
	private val mangaData = MutableStateFlow(intent.manga)
	private val selectedBranch = MutableStateFlow<String?>(null)

	private val history = mangaData.mapNotNull { it?.id }
		.distinctUntilChanged()
		.flatMapLatest { mangaId ->
			historyRepository.observeOne(mangaId)
		}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, null)

	private val favourite = mangaData.mapNotNull { it?.id }
		.distinctUntilChanged()
		.flatMapLatest { mangaId ->
			favouritesRepository.observeCategoriesIds(mangaId).map { it.isNotEmpty() }
		}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, false)

	private val newChapters = mangaData.mapNotNull { it?.id }
		.distinctUntilChanged()
		.mapLatest { mangaId ->
			trackingRepository.getNewChaptersCount(mangaId)
		}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, 0)

	private val remoteManga = MutableStateFlow<Manga?>(null)
	private val chaptersQuery = MutableStateFlow("")

	private val chaptersReversed = settings.observe()
		.filter { it == AppSettings.KEY_REVERSE_CHAPTERS }
		.map { settings.chaptersReverse }
		.onStart { emit(settings.chaptersReverse) }
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, false)

	val manga = mangaData.filterNotNull()
		.asLiveData(viewModelScope.coroutineContext)
	val favouriteCategories = favourite
		.asLiveData(viewModelScope.coroutineContext)
	val newChaptersCount = newChapters
		.asLiveData(viewModelScope.coroutineContext)
	val readingHistory = history
		.asLiveData(viewModelScope.coroutineContext)
	val isChaptersReversed = chaptersReversed
		.asLiveData(viewModelScope.coroutineContext)

	val onMangaRemoved = SingleLiveEvent<Manga>()

	val branches = mangaData.map {
		it?.chapters?.mapToSet { x -> x.branch }?.sortedBy { x -> x }.orEmpty()
	}.asLiveData(viewModelScope.coroutineContext + Dispatchers.Default)

	val selectedBranchIndex = combine(
		branches.asFlow(),
		selectedBranch
	) { branches, selected ->
		branches.indexOf(selected)
	}.asLiveData(viewModelScope.coroutineContext + Dispatchers.Default)

	val hasChapters = mangaData.map {
		!(it?.chapters.isNullOrEmpty())
	}.asLiveData(viewModelScope.coroutineContext + Dispatchers.Default)

	val chapters = combine(
		combine(
			mangaData.map { it?.chapters.orEmpty() },
			remoteManga,
			history.map { it?.chapterId },
			newChapters,
			selectedBranch
		) { chapters, sourceManga, currentId, newCount, branch ->
			val sourceChapters = sourceManga?.chapters
			if (sourceManga?.source != MangaSource.LOCAL && !sourceChapters.isNullOrEmpty()) {
				mapChaptersWithSource(chapters, sourceChapters, currentId, newCount, branch)
			} else {
				mapChapters(chapters, sourceChapters, currentId, newCount, branch)
			}
		},
		chaptersReversed,
		chaptersQuery,
	) { list, reversed, query ->
		(if (reversed) list.asReversed() else list).filterSearch(query)
	}.asLiveData(viewModelScope.coroutineContext + Dispatchers.Default)

	init {
		loadingJob = doLoad()
	}

	fun reload() {
		loadingJob.cancel()
		loadingJob = doLoad()
	}

	fun deleteLocal(manga: Manga) {
		launchLoadingJob(Dispatchers.Default) {
			val original = localMangaRepository.getRemoteManga(manga)
			localMangaRepository.delete(manga) || throw IOException("Unable to delete file")
			runCatching {
				historyRepository.deleteOrSwap(manga, original)
			}
			onMangaRemoved.postCall(manga)
		}
	}

	fun setChaptersReversed(newValue: Boolean) {
		settings.chaptersReverse = newValue
	}

	fun setSelectedBranch(branch: String?) {
		selectedBranch.value = branch
	}

	fun getRemoteManga(): Manga? {
		return remoteManga.value
	}

	fun performChapterSearch(query: String?) {
		chaptersQuery.value = query?.trim().orEmpty()
	}

	private fun doLoad() = launchLoadingJob(Dispatchers.Default) {
		var manga = mangaDataRepository.resolveIntent(intent)
			?: throw MangaNotFoundException("Cannot find manga")
		mangaData.value = manga
		manga = MangaRepository(manga.source).getDetails(manga)
		// find default branch
		val hist = historyRepository.getOne(manga)
		selectedBranch.value = if (hist != null) {
			manga.chapters?.find { it.id == hist.chapterId }?.branch
		} else {
			predictBranch(manga.chapters)
		}
		mangaData.value = manga
		remoteManga.value = runCatching {
			if (manga.source == MangaSource.LOCAL) {
				val m = localMangaRepository.getRemoteManga(manga) ?: return@runCatching null
				MangaRepository(m.source).getDetails(m)
			} else {
				localMangaRepository.findSavedManga(manga)
			}
		}.getOrNull()
	}

	private fun mapChapters(
		chapters: List<MangaChapter>,
		downloadedChapters: List<MangaChapter>?,
		currentId: Long?,
		newCount: Int,
		branch: String?,
	): List<ChapterListItem> {
		val result = ArrayList<ChapterListItem>(chapters.size)
		val dateFormat = settings.getDateFormat()
		val currentIndex = chapters.indexOfFirst { it.id == currentId }
		val firstNewIndex = chapters.size - newCount
		val downloadedIds = downloadedChapters?.mapToSet { it.id }
		for (i in chapters.indices) {
			val chapter = chapters[i]
			if (chapter.branch != branch) {
				continue
			}
			result += chapter.toListItem(
				isCurrent = i == currentIndex,
				isUnread = i > currentIndex,
				isNew = i >= firstNewIndex,
				isMissing = false,
				isDownloaded = downloadedIds?.contains(chapter.id) == true,
				dateFormat = dateFormat,
			)
		}
		return result
	}

	private fun mapChaptersWithSource(
		chapters: List<MangaChapter>,
		sourceChapters: List<MangaChapter>,
		currentId: Long?,
		newCount: Int,
		branch: String?,
	): List<ChapterListItem> {
		val chaptersMap = chapters.associateByTo(HashMap(chapters.size)) { it.id }
		val result = ArrayList<ChapterListItem>(sourceChapters.size)
		val currentIndex = sourceChapters.indexOfFirst { it.id == currentId }
		val firstNewIndex = sourceChapters.size - newCount
		val dateFormat = settings.getDateFormat()
		for (i in sourceChapters.indices) {
			val chapter = sourceChapters[i]
			if (chapter.branch != branch) {
				continue
			}
			val localChapter = chaptersMap.remove(chapter.id)
			result += localChapter?.toListItem(
				isCurrent = i == currentIndex,
				isUnread = i > currentIndex,
				isNew = i >= firstNewIndex,
				isMissing = false,
				isDownloaded = false,
				dateFormat = dateFormat,
			) ?: chapter.toListItem(
				isCurrent = i == currentIndex,
				isUnread = i > currentIndex,
				isNew = i >= firstNewIndex,
				isMissing = true,
				isDownloaded = false,
				dateFormat = dateFormat,
			)
		}
		if (chaptersMap.isNotEmpty()) { // some chapters on device but not online source
			result.ensureCapacity(result.size + chaptersMap.size)
			chaptersMap.values.mapTo(result) {
				it.toListItem(
					isCurrent = false,
					isUnread = true,
					isNew = false,
					isMissing = false,
					isDownloaded = false,
					dateFormat = dateFormat,
				)
			}
			result.sortBy { it.chapter.number }
		}
		return result
	}

	private fun predictBranch(chapters: List<MangaChapter>?): String? {
		if (chapters.isNullOrEmpty()) {
			return null
		}
		val groups = chapters.groupBy { it.branch }
		for (locale in LocaleListCompat.getAdjustedDefault()) {
			var language = locale.getDisplayLanguage(locale).toTitleCase(locale)
			if (groups.containsKey(language)) {
				return language
			}
			language = locale.getDisplayName(locale).toTitleCase(locale)
			if (groups.containsKey(language)) {
				return language
			}
		}
		return groups.maxByOrNull { it.value.size }?.key
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