package org.koitharu.kotatsu.shelf.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.favourites.data.FavouriteCategoryEntity
import org.koitharu.kotatsu.favourites.data.toFavouriteCategory
import org.koitharu.kotatsu.favourites.data.toMangaList
import org.koitharu.kotatsu.history.data.HistoryRepository
import org.koitharu.kotatsu.local.data.LocalMangaRepository
import org.koitharu.kotatsu.local.data.LocalStorageChanges
import org.koitharu.kotatsu.local.domain.model.LocalManga
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.SortOrder
import org.koitharu.kotatsu.shelf.domain.model.ShelfContent
import org.koitharu.kotatsu.suggestions.domain.SuggestionRepository
import org.koitharu.kotatsu.tracker.domain.TrackingRepository
import javax.inject.Inject

@Suppress("SameParameterValue")
class ShelfContentObserveUseCase @Inject constructor(
	private val localMangaRepository: LocalMangaRepository,
	private val historyRepository: HistoryRepository,
	private val trackingRepository: TrackingRepository,
	private val suggestionRepository: SuggestionRepository,
	private val db: MangaDatabase,
	@LocalStorageChanges private val localStorageChanges: SharedFlow<LocalManga?>,
) {

	operator fun invoke(): Flow<ShelfContent> = combine(
		historyRepository.observeAll(20),
		observeLocalManga(SortOrder.UPDATED, 20),
		observeFavourites(),
		trackingRepository.observeUpdatedManga(),
		suggestionRepository.observeAll(20),
	) { history, local, favorites, updated, suggestions ->
		ShelfContent(history, favorites, updated, local, suggestions)
	}

	private fun observeLocalManga(sortOrder: SortOrder, limit: Int): Flow<List<Manga>> {
		return localStorageChanges
			.onStart { emit(null) }
			.mapLatest {
				localMangaRepository.getList(0, null, sortOrder).take(limit)
			}.distinctUntilChanged()
	}

	private fun observeFavourites(): Flow<Map<FavouriteCategory, List<Manga>>> {
		return db.favouriteCategoriesDao.observeAll()
			.flatMapLatest { categories ->
				val cats = categories.filter { it.isVisibleInLibrary }
				if (cats.isEmpty()) {
					flowOf(emptyMap())
				} else {
					observeCategoriesContent(cats)
				}
			}
	}

	private fun observeCategoriesContent(
		categories: List<FavouriteCategoryEntity>,
	) = combine<Pair<FavouriteCategory, List<Manga>>, Map<FavouriteCategory, List<Manga>>>(
		categories.map { cat ->
			val category = cat.toFavouriteCategory()
			db.favouritesDao.observeAll(category.id, category.order)
				.map { category to it.toMangaList() }
		},
	) { array -> array.toMap() }
}
