package org.koitharu.kotatsu.favourites.domain

import androidx.room.withTransaction
import dagger.Reusable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.db.entity.toEntities
import org.koitharu.kotatsu.core.db.entity.toEntity
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.core.ui.util.ReversibleHandle
import org.koitharu.kotatsu.core.util.ext.mapItems
import org.koitharu.kotatsu.favourites.data.FavouriteCategoryEntity
import org.koitharu.kotatsu.favourites.data.FavouriteEntity
import org.koitharu.kotatsu.favourites.data.toFavouriteCategory
import org.koitharu.kotatsu.favourites.data.toManga
import org.koitharu.kotatsu.favourites.data.toMangaList
import org.koitharu.kotatsu.favourites.domain.model.Cover
import org.koitharu.kotatsu.list.domain.ListFilterOption
import org.koitharu.kotatsu.list.domain.ListSortOrder
import org.koitharu.kotatsu.parsers.model.Manga
import javax.inject.Inject

@Reusable
class FavouritesRepository @Inject constructor(
	private val db: MangaDatabase,
) {

	suspend fun getAllManga(): List<Manga> {
		val entities = db.getFavouritesDao().findAll()
		return entities.toMangaList()
	}

	suspend fun getLastManga(limit: Int): List<Manga> {
		val entities = db.getFavouritesDao().findLast(limit)
		return entities.toMangaList()
	}

	fun observeAll(order: ListSortOrder, filterOptions: Set<ListFilterOption>, limit: Int): Flow<List<Manga>> {
		return db.getFavouritesDao().observeAll(order, filterOptions, limit)
			.mapItems { it.toManga() }
	}

	suspend fun getManga(categoryId: Long): List<Manga> {
		val entities = db.getFavouritesDao().findAll(categoryId)
		return entities.toMangaList()
	}

	fun observeAll(
		categoryId: Long,
		order: ListSortOrder,
		filterOptions: Set<ListFilterOption>,
		limit: Int
	): Flow<List<Manga>> {
		return db.getFavouritesDao().observeAll(categoryId, order, filterOptions, limit)
			.mapItems { it.toManga() }
	}

	fun observeAll(categoryId: Long, filterOptions: Set<ListFilterOption>, limit: Int): Flow<List<Manga>> {
		return observeOrder(categoryId)
			.flatMapLatest { order -> observeAll(categoryId, order, filterOptions, limit) }
	}

	fun observeMangaCount(): Flow<Int> {
		return db.getFavouritesDao().observeMangaCount()
			.distinctUntilChanged()
	}

	fun observeCategories(): Flow<List<FavouriteCategory>> {
		return db.getFavouriteCategoriesDao().observeAll().mapItems {
			it.toFavouriteCategory()
		}.distinctUntilChanged()
	}

	fun observeCategoriesForLibrary(): Flow<List<FavouriteCategory>> {
		return db.getFavouriteCategoriesDao().observeAllVisible().mapItems {
			it.toFavouriteCategory()
		}.distinctUntilChanged()
	}

	fun observeCategoriesWithCovers(): Flow<Map<FavouriteCategory, List<Cover>>> {
		return db.getFavouriteCategoriesDao().observeAll()
			.map {
				db.withTransaction {
					val res = LinkedHashMap<FavouriteCategory, List<Cover>>()
					for (entity in it) {
						val cat = entity.toFavouriteCategory()
						res[cat] = db.getFavouritesDao().findCovers(
							categoryId = cat.id,
							order = cat.order,
						)
					}
					res
				}
			}
	}

	suspend fun getAllFavoritesCovers(order: ListSortOrder, limit: Int): List<Cover> {
		return db.getFavouritesDao().findCovers(order, limit)
	}

	fun observeCategory(id: Long): Flow<FavouriteCategory?> {
		return db.getFavouriteCategoriesDao().observe(id)
			.map { it?.toFavouriteCategory() }
	}

	fun observeCategoriesIds(mangaId: Long): Flow<Set<Long>> {
		return db.getFavouritesDao().observeIds(mangaId).map { it.toSet() }
	}

	fun observeCategories(mangaId: Long): Flow<Set<FavouriteCategory>> {
		return db.getFavouritesDao().observeCategories(mangaId).map {
			it.mapTo(LinkedHashSet(it.size)) { x -> x.toFavouriteCategory() }
		}
	}

	suspend fun getCategory(id: Long): FavouriteCategory {
		return db.getFavouriteCategoriesDao().find(id.toInt()).toFavouriteCategory()
	}

	suspend fun isFavorite(mangaId: Long): Boolean {
		return db.getFavouritesDao().findCategoriesCount(mangaId) != 0
	}

	@Deprecated("")
	suspend fun getCategoriesIds(mangaIds: Collection<Long>): Set<Long> {
		return db.getFavouritesDao().findCategoriesIds(mangaIds).toSet()
	}

	suspend fun getCategoriesIds(mangaId: Long): Set<Long> {
		return db.getFavouritesDao().findCategoriesIds(mangaId).toSet()
	}

	suspend fun createCategory(
		title: String,
		sortOrder: ListSortOrder,
		isTrackerEnabled: Boolean,
		isVisibleOnShelf: Boolean,
	): FavouriteCategory {
		val entity = FavouriteCategoryEntity(
			title = title,
			createdAt = System.currentTimeMillis(),
			sortKey = db.getFavouriteCategoriesDao().getNextSortKey(),
			categoryId = 0,
			order = sortOrder.name,
			track = isTrackerEnabled,
			deletedAt = 0L,
			isVisibleInLibrary = isVisibleOnShelf,
		)
		val id = db.getFavouriteCategoriesDao().insert(entity)
		val category = entity.toFavouriteCategory(id)
		return category
	}

	suspend fun updateCategory(
		id: Long,
		title: String,
		sortOrder: ListSortOrder,
		isTrackerEnabled: Boolean,
		isVisibleOnShelf: Boolean,
	) {
		db.getFavouriteCategoriesDao().update(id, title, sortOrder.name, isTrackerEnabled, isVisibleOnShelf)
	}

	suspend fun updateCategory(id: Long, isVisibleInLibrary: Boolean) {
		db.getFavouriteCategoriesDao().updateVisibility(id, isVisibleInLibrary)
	}

	suspend fun updateCategoryTracking(id: Long, isTrackingEnabled: Boolean) {
		db.getFavouriteCategoriesDao().updateTracking(id, isTrackingEnabled)
	}

	suspend fun removeCategories(ids: Collection<Long>) {
		db.withTransaction {
			for (id in ids) {
				db.getFavouritesDao().deleteAll(id)
				db.getFavouriteCategoriesDao().delete(id)
			}
		}
	}

	suspend fun setCategoryOrder(id: Long, order: ListSortOrder) {
		db.getFavouriteCategoriesDao().updateOrder(id, order.name)
	}

	suspend fun reorderCategories(orderedIds: List<Long>) {
		val dao = db.getFavouriteCategoriesDao()
		db.withTransaction {
			for ((i, id) in orderedIds.withIndex()) {
				dao.updateSortKey(id, i)
			}
		}
	}

	suspend fun addToCategory(categoryId: Long, mangas: Collection<Manga>) {
		db.withTransaction {
			for (manga in mangas) {
				val tags = manga.tags.toEntities()
				db.getTagsDao().upsert(tags)
				db.getMangaDao().upsert(manga.toEntity(), tags)
				val entity = FavouriteEntity(
					mangaId = manga.id,
					categoryId = categoryId,
					createdAt = System.currentTimeMillis(),
					sortKey = 0,
					deletedAt = 0L,
				)
				db.getFavouritesDao().insert(entity)
			}
		}
	}

	suspend fun removeFromFavourites(ids: Collection<Long>): ReversibleHandle {
		db.withTransaction {
			for (id in ids) {
				db.getFavouritesDao().delete(mangaId = id)
			}
		}
		return ReversibleHandle { recoverToFavourites(ids) }
	}

	suspend fun removeFromCategory(categoryId: Long, ids: Collection<Long>): ReversibleHandle {
		db.withTransaction {
			for (id in ids) {
				db.getFavouritesDao().delete(categoryId = categoryId, mangaId = id)
			}
		}
		return ReversibleHandle { recoverToCategory(categoryId, ids) }
	}

	private fun observeOrder(categoryId: Long): Flow<ListSortOrder> {
		return db.getFavouriteCategoriesDao().observe(categoryId)
			.filterNotNull()
			.map { x -> ListSortOrder(x.order, ListSortOrder.NEWEST) }
			.distinctUntilChanged()
	}

	suspend fun getMostUpdatedCategories(limit: Int): List<FavouriteCategory> {
		return db.getFavouriteCategoriesDao().getMostUpdatedCategories(limit).map {
			it.toFavouriteCategory()
		}
	}

	private suspend fun recoverToFavourites(ids: Collection<Long>) {
		db.withTransaction {
			for (id in ids) {
				db.getFavouritesDao().recover(mangaId = id)
			}
		}
	}

	private suspend fun recoverToCategory(categoryId: Long, ids: Collection<Long>) {
		db.withTransaction {
			for (id in ids) {
				db.getFavouritesDao().recover(mangaId = id, categoryId = categoryId)
			}
		}
	}
}
