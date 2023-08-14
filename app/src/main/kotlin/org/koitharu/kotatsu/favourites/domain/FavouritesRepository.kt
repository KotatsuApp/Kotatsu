package org.koitharu.kotatsu.favourites.domain

import androidx.room.withTransaction
import dagger.Reusable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.db.entity.SortOrder
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
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.SortOrder
import org.koitharu.kotatsu.tracker.work.TrackerNotificationChannels
import javax.inject.Inject

@Reusable
class FavouritesRepository @Inject constructor(
	private val db: MangaDatabase,
	private val channels: TrackerNotificationChannels,
) {

	suspend fun getAllManga(): List<Manga> {
		val entities = db.favouritesDao.findAll()
		return entities.toMangaList()
	}

	suspend fun getLastManga(limit: Int): List<Manga> {
		val entities = db.favouritesDao.findLast(limit)
		return entities.toMangaList()
	}

	fun observeAll(order: SortOrder): Flow<List<Manga>> {
		return db.favouritesDao.observeAll(order)
			.mapItems { it.toManga() }
	}

	suspend fun getManga(categoryId: Long): List<Manga> {
		val entities = db.favouritesDao.findAll(categoryId)
		return entities.toMangaList()
	}

	fun observeAll(categoryId: Long, order: SortOrder): Flow<List<Manga>> {
		return db.favouritesDao.observeAll(categoryId, order)
			.mapItems { it.toManga() }
	}

	fun observeAll(categoryId: Long): Flow<List<Manga>> {
		return observeOrder(categoryId)
			.flatMapLatest { order -> observeAll(categoryId, order) }
	}

	fun observeCategories(): Flow<List<FavouriteCategory>> {
		return db.favouriteCategoriesDao.observeAll().mapItems {
			it.toFavouriteCategory()
		}.distinctUntilChanged()
	}

	fun observeCategoriesForLibrary(): Flow<List<FavouriteCategory>> {
		return db.favouriteCategoriesDao.observeAllForLibrary().mapItems {
			it.toFavouriteCategory()
		}.distinctUntilChanged()
	}

	fun observeCategoriesWithCovers(): Flow<Map<FavouriteCategory, List<Cover>>> {
		return db.favouriteCategoriesDao.observeAll()
			.map {
				db.withTransaction {
					val res = LinkedHashMap<FavouriteCategory, List<Cover>>()
					for (entity in it) {
						val cat = entity.toFavouriteCategory()
						res[cat] = db.favouritesDao.findCovers(
							categoryId = cat.id,
							order = cat.order,
						)
					}
					res
				}
			}
	}

	fun observeCategory(id: Long): Flow<FavouriteCategory?> {
		return db.favouriteCategoriesDao.observe(id)
			.map { it?.toFavouriteCategory() }
	}

	fun observeCategoriesIds(mangaId: Long): Flow<Set<Long>> {
		return db.favouritesDao.observeIds(mangaId).map { it.toSet() }
	}

	suspend fun getCategory(id: Long): FavouriteCategory {
		return db.favouriteCategoriesDao.find(id.toInt()).toFavouriteCategory()
	}

	suspend fun createCategory(
		title: String,
		sortOrder: SortOrder,
		isTrackerEnabled: Boolean,
		isVisibleOnShelf: Boolean,
	): FavouriteCategory {
		val entity = FavouriteCategoryEntity(
			title = title,
			createdAt = System.currentTimeMillis(),
			sortKey = db.favouriteCategoriesDao.getNextSortKey(),
			categoryId = 0,
			order = sortOrder.name,
			track = isTrackerEnabled,
			deletedAt = 0L,
			isVisibleInLibrary = isVisibleOnShelf,
		)
		val id = db.favouriteCategoriesDao.insert(entity)
		val category = entity.toFavouriteCategory(id)
		channels.createChannel(category)
		return category
	}

	suspend fun updateCategory(
		id: Long,
		title: String,
		sortOrder: SortOrder,
		isTrackerEnabled: Boolean,
		isVisibleOnShelf: Boolean,
	) {
		db.favouriteCategoriesDao.update(id, title, sortOrder.name, isTrackerEnabled, isVisibleOnShelf)
	}

	suspend fun updateCategory(id: Long, isVisibleInLibrary: Boolean) {
		db.favouriteCategoriesDao.updateLibVisibility(id, isVisibleInLibrary)
	}

	suspend fun updateCategoryTracking(id: Long, isTrackingEnabled: Boolean) {
		db.favouriteCategoriesDao.updateTracking(id, isTrackingEnabled)
	}

	suspend fun removeCategories(ids: Collection<Long>) {
		db.withTransaction {
			for (id in ids) {
				db.favouritesDao.deleteAll(id)
				db.favouriteCategoriesDao.delete(id)
			}
		}
		// run after transaction success
		for (id in ids) {
			channels.deleteChannel(id)
		}
	}

	suspend fun setCategoryOrder(id: Long, order: SortOrder) {
		db.favouriteCategoriesDao.updateOrder(id, order.name)
	}

	suspend fun reorderCategories(orderedIds: List<Long>) {
		val dao = db.favouriteCategoriesDao
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
				db.tagsDao.upsert(tags)
				db.mangaDao.upsert(manga.toEntity(), tags)
				val entity = FavouriteEntity(
					mangaId = manga.id,
					categoryId = categoryId,
					createdAt = System.currentTimeMillis(),
					sortKey = 0,
					deletedAt = 0L,
				)
				db.favouritesDao.insert(entity)
			}
		}
	}

	suspend fun removeFromFavourites(ids: Collection<Long>): ReversibleHandle {
		db.withTransaction {
			for (id in ids) {
				db.favouritesDao.delete(mangaId = id)
			}
		}
		return ReversibleHandle { recoverToFavourites(ids) }
	}

	suspend fun removeFromCategory(categoryId: Long, ids: Collection<Long>): ReversibleHandle {
		db.withTransaction {
			for (id in ids) {
				db.favouritesDao.delete(categoryId = categoryId, mangaId = id)
			}
		}
		return ReversibleHandle { recoverToCategory(categoryId, ids) }
	}

	private fun observeOrder(categoryId: Long): Flow<SortOrder> {
		return db.favouriteCategoriesDao.observe(categoryId)
			.filterNotNull()
			.map { x -> SortOrder(x.order, SortOrder.NEWEST) }
			.distinctUntilChanged()
	}

	private suspend fun recoverToFavourites(ids: Collection<Long>) {
		db.withTransaction {
			for (id in ids) {
				db.favouritesDao.recover(mangaId = id)
			}
		}
	}

	private suspend fun recoverToCategory(categoryId: Long, ids: Collection<Long>) {
		db.withTransaction {
			for (id in ids) {
				db.favouritesDao.recover(mangaId = id, categoryId = categoryId)
			}
		}
	}
}
