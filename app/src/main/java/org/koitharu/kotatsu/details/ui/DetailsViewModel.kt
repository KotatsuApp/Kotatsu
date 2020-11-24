package org.koitharu.kotatsu.details.ui

import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import org.koitharu.kotatsu.base.domain.MangaDataRepository
import org.koitharu.kotatsu.base.domain.MangaIntent
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.exceptions.MangaNotFoundException
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.details.ui.model.toListItem
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository
import org.koitharu.kotatsu.history.domain.ChapterExtra
import org.koitharu.kotatsu.history.domain.HistoryRepository
import org.koitharu.kotatsu.local.domain.LocalMangaRepository
import org.koitharu.kotatsu.tracker.domain.TrackingRepository
import org.koitharu.kotatsu.utils.SingleLiveEvent
import org.koitharu.kotatsu.utils.ext.safe
import java.io.IOException

class DetailsViewModel(
	intent: MangaIntent,
	private val historyRepository: HistoryRepository,
	private val favouritesRepository: FavouritesRepository,
	private val localMangaRepository: LocalMangaRepository,
	private val trackingRepository: TrackingRepository,
	private val mangaDataRepository: MangaDataRepository
) : BaseViewModel() {

	private val mangaData = MutableStateFlow<Manga?>(intent.manga)

	private val history = mangaData.mapNotNull { it?.id }
		.distinctUntilChanged()
		.flatMapLatest { mangaId ->
			historyRepository.observeOne(mangaId)
		}.stateIn(viewModelScope, SharingStarted.Eagerly, null)

	private val favourite = mangaData.mapNotNull { it?.id }
		.distinctUntilChanged()
		.flatMapLatest { mangaId ->
			favouritesRepository.observeCategories(mangaId)
		}.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

	private val newChapters = mangaData.mapNotNull { it?.id }
		.distinctUntilChanged()
		.mapLatest { mangaId ->
			trackingRepository.getNewChaptersCount(mangaId)
		}.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

	val manga = mangaData.filterNotNull()
		.asLiveData(viewModelScope.coroutineContext + Dispatchers.Default)
	val favouriteCategories = favourite
		.asLiveData(viewModelScope.coroutineContext + Dispatchers.Default)
	val newChaptersCount = newChapters
		.asLiveData(viewModelScope.coroutineContext + Dispatchers.Default)
	val readingHistory = history
		.asLiveData(viewModelScope.coroutineContext + Dispatchers.Default)

	val onMangaRemoved = SingleLiveEvent<Manga>()

	val chapters = combine(
		mangaData.map { it?.chapters.orEmpty() },
		history.map { it?.chapterId },
		newChapters
	) { chapters, currentId, newCount ->
		val currentIndex = chapters.indexOfFirst { it.id == currentId }
		val firstNewIndex = chapters.size - newCount
		chapters.mapIndexed { index, chapter ->
			chapter.toListItem(
				when {
					index >= firstNewIndex -> ChapterExtra.NEW
					index == currentIndex -> ChapterExtra.CURRENT
					index < currentIndex -> ChapterExtra.READ
					else -> ChapterExtra.UNREAD
				}
			)
		}
	}.asLiveData(viewModelScope.coroutineContext + Dispatchers.Default)

	init {
		launchLoadingJob(Dispatchers.Default) {
			var manga = mangaDataRepository.resolveIntent(intent)
				?: throw MangaNotFoundException("Cannot find manga")
			mangaData.value = manga
			manga = manga.source.repository.getDetails(manga)
			mangaData.value = manga
		}
	}

	fun deleteLocal(manga: Manga) {
		launchLoadingJob(Dispatchers.Default) {
			val original = localMangaRepository.getRemoteManga(manga)
			localMangaRepository.delete(manga) || throw IOException("Unable to delete file")
			safe {
				historyRepository.deleteOrSwap(manga, original)
			}
			onMangaRemoved.postCall(manga)
		}
	}
}