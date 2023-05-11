package org.koitharu.kotatsu.shelf.domain

import dagger.Reusable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
import org.koitharu.kotatsu.core.db.entity.toManga
import org.koitharu.kotatsu.core.db.entity.toMangaTags
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.favourites.data.FavouriteCategoryEntity
import org.koitharu.kotatsu.favourites.data.toFavouriteCategory
import org.koitharu.kotatsu.history.domain.HistoryRepository
import org.koitharu.kotatsu.local.data.LocalManga
import org.koitharu.kotatsu.local.data.LocalStorageChanges
import org.koitharu.kotatsu.local.domain.LocalMangaRepository
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.SortOrder
import org.koitharu.kotatsu.suggestions.domain.SuggestionRepository
import org.koitharu.kotatsu.tracker.domain.TrackingRepository
import org.koitharu.kotatsu.utils.ext.runCatchingCancellable
import javax.inject.Inject

@Reusable
class ShelfRepository @Inject constructor(
	private val localMangaRepository: LocalMangaRepository,
	private val historyRepository: HistoryRepository,
	private val trackingRepository: TrackingRepository,
	private val suggestionRepository: SuggestionRepository,
	private val db: MangaDatabase,
	@LocalStorageChanges private val localStorageChanges: SharedFlow<LocalManga?>,
) {

	fun observeShelfContent(): Flow<ShelfContent> = combine(
		historyRepository.observeAllWithHistory(),
		observeLocalManga(SortOrder.UPDATED),
		observeFavourites(),
		trackingRepository.observeUpdatedManga(),
		suggestionRepository.observeAll(16),
	) { history, local, favorites, updated, suggestions ->
		ShelfContent(history, favorites, updated, local, suggestions)
	}

	private fun observeLocalManga(sortOrder: SortOrder): Flow<List<Manga>> {
		return localStorageChanges
			.onStart { emit(null) }
			.mapLatest {
				localMangaRepository.getList(0, null, sortOrder)
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

	suspend fun deleteLocalManga(ids: Set<Long>) {
		val list = localMangaRepository.getList(0, null, null)
			.filter { x -> x.id in ids }
		coroutineScope {
			list.map { manga ->
				async {
					val original = localMangaRepository.getRemoteManga(manga)
					if (localMangaRepository.delete(manga)) {
						runCatchingCancellable {
							historyRepository.deleteOrSwap(manga, original)
						}
					}
				}
			}.awaitAll()
		}
	}

	private fun observeCategoriesContent(
		categories: List<FavouriteCategoryEntity>,
	) = combine<Pair<FavouriteCategory, List<Manga>>, Map<FavouriteCategory, List<Manga>>>(
		categories.map { cat ->
			val category = cat.toFavouriteCategory()
			db.favouritesDao.observeAll(category.id, category.order)
				.map { category to it.map { x -> x.manga.toManga(x.tags.toMangaTags()) } }
		},
	) { array -> array.toMap() }
}
