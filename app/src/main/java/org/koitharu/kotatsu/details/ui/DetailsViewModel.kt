package org.koitharu.kotatsu.details.ui

import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.base.domain.MangaDataRepository
import org.koitharu.kotatsu.base.domain.MangaIntent
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.exceptions.MangaNotFoundException
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaChapter
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.details.ui.model.ChapterListItem
import org.koitharu.kotatsu.details.ui.model.toListItem
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository
import org.koitharu.kotatsu.history.domain.ChapterExtra
import org.koitharu.kotatsu.history.domain.HistoryRepository
import org.koitharu.kotatsu.local.domain.LocalMangaRepository
import org.koitharu.kotatsu.tracker.domain.TrackingRepository
import org.koitharu.kotatsu.utils.SingleLiveEvent
import org.koitharu.kotatsu.utils.ext.mapToSet
import org.koitharu.kotatsu.utils.ext.toTitleCase
import java.io.IOException
import java.util.*

class DetailsViewModel(
	intent: MangaIntent,
	private val historyRepository: HistoryRepository,
	private val favouritesRepository: FavouritesRepository,
	private val localMangaRepository: LocalMangaRepository,
	private val trackingRepository: TrackingRepository,
	private val mangaDataRepository: MangaDataRepository,
	private val settings: AppSettings,
) : BaseViewModel() {

	private val mangaData = MutableStateFlow<Manga?>(intent.manga)
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
	/*private val remoteManga = mangaData.mapLatest {
		if (it?.source == MangaSource.LOCAL) {
			runCatching {
				val m = localMangaRepository.getRemoteManga(it) ?: return@mapLatest null
				MangaRepository(m.source).getDetails(m)
			}.getOrNull()
		} else {
			null
		}
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, null)*/

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

	val chapters = combine(
		mangaData.map { it?.chapters.orEmpty() },
		remoteManga,
		history.map { it?.chapterId },
		newChapters,
		selectedBranch
	) { chapters, sourceManga, currentId, newCount, branch ->
		val sourceChapters = sourceManga?.chapters
		if (sourceChapters.isNullOrEmpty()) {
			mapChapters(chapters, currentId, newCount, branch)
		} else {
			mapChaptersWithSource(chapters, sourceChapters, currentId, newCount, branch)
		}
	}.combine(chaptersReversed) { list, reversed ->
		if (reversed) list.asReversed() else list
	}.asLiveData(viewModelScope.coroutineContext + Dispatchers.Default)

	init {
		launchLoadingJob(Dispatchers.Default) {
			var manga = mangaDataRepository.resolveIntent(intent)
				?: throw MangaNotFoundException("Cannot find manga")
			mangaData.value = manga
			manga = manga.source.repository.getDetails(manga)
			// find default branch
			val hist = historyRepository.getOne(manga)
			selectedBranch.value = if (hist != null) {
				manga.chapters?.find { it.id == hist.chapterId }?.branch
			} else {
				predictBranch(manga.chapters)
			}
			mangaData.value = manga
			if (manga.source == MangaSource.LOCAL) {
				remoteManga.value = runCatching {
					val m = localMangaRepository.getRemoteManga(manga) ?: return@runCatching null
					MangaRepository(m.source).getDetails(m)
				}.getOrNull()
			}
		}
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

	private fun mapChapters(
		chapters: List<MangaChapter>,
		currentId: Long?,
		newCount: Int,
		branch: String?,
	): List<ChapterListItem> {
		val result = ArrayList<ChapterListItem>(chapters.size)
		val dateFormat = settings.dateFormat()
		val currentIndex = chapters.indexOfFirst { it.id == currentId }
		val firstNewIndex = chapters.size - newCount
		for (i in chapters.indices) {
			val chapter = chapters[i]
			if (chapter.branch != branch) {
				continue
			}
			result += chapter.toListItem(
				extra = when {
					i >= firstNewIndex -> ChapterExtra.NEW
					i == currentIndex -> ChapterExtra.CURRENT
					i < currentIndex -> ChapterExtra.READ
					else -> ChapterExtra.UNREAD
				},
				isMissing = false,
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
		val dateFormat = settings.dateFormat()
		for (i in sourceChapters.indices) {
			val chapter = sourceChapters[i]
			if (chapter.branch != branch) {
				continue
			}
			val localChapter = chaptersMap.remove(chapter.id)
			result += localChapter?.toListItem(
				extra = when {
					i >= firstNewIndex -> ChapterExtra.NEW
					i == currentIndex -> ChapterExtra.CURRENT
					i < currentIndex -> ChapterExtra.READ
					else -> ChapterExtra.UNREAD
				},
				isMissing = false,
				dateFormat = dateFormat,
			) ?: chapter.toListItem(
				extra = when {
					i >= firstNewIndex -> ChapterExtra.NEW
					i == currentIndex -> ChapterExtra.CURRENT
					i < currentIndex -> ChapterExtra.READ
					else -> ChapterExtra.UNREAD
				},
				isMissing = true,
				dateFormat = dateFormat,
			)
		}
		if (chaptersMap.isNotEmpty()) { // some chapters on device but not online source
			result.ensureCapacity(result.size + chaptersMap.size)
			chaptersMap.values.mapTo(result) {
				it.toListItem(ChapterExtra.UNREAD, false, dateFormat)
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
		val locale = Locale.getDefault()
		var language = locale.displayLanguage.toTitleCase(locale)
		if (groups.containsKey(language)) {
			return language
		}
		language = locale.displayName.toTitleCase(locale)
		if (groups.containsKey(language)) {
			return language
		}
		return groups.maxByOrNull { it.value.size }?.key
	}
}