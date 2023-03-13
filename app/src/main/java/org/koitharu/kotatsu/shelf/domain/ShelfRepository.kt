package org.koitharu.kotatsu.shelf.domain

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.db.entity.toManga
import org.koitharu.kotatsu.core.db.entity.toMangaTags
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.favourites.data.FavouriteCategoryEntity
import org.koitharu.kotatsu.favourites.data.toFavouriteCategory
import org.koitharu.kotatsu.history.domain.HistoryRepository
import org.koitharu.kotatsu.local.domain.LocalMangaRepository
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.SortOrder
import org.koitharu.kotatsu.tracker.domain.TrackingRepository
import org.koitharu.kotatsu.utils.ext.runCatchingCancellable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShelfRepository @Inject constructor(
	private val localMangaRepository: LocalMangaRepository,
	private val historyRepository: HistoryRepository,
	private val trackingRepository: TrackingRepository,
	private val db: MangaDatabase,
) {

	fun observeShelfContent(): Flow<ShelfContent> = combine(
		historyRepository.observeAllWithHistory(),
		observeLocalManga(SortOrder.UPDATED),
		observeFavourites(),
		trackingRepository.observeUpdatedManga(),
	) { history, local, favorites, updated ->
		ShelfContent(history, favorites, updated, local)
	}

	fun observeLocalManga(sortOrder: SortOrder): Flow<List<Manga>> {
		return flow {
			emit(null)
			emitAll(localMangaRepository.watchReadableDirs())
		}.mapLatest {
			localMangaRepository.getList(0, null, sortOrder)
		}
	}

	fun observeFavourites(): Flow<Map<FavouriteCategory, List<Manga>>> {
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
