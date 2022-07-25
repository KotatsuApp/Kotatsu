package org.koitharu.kotatsu.favourites.domain

import androidx.room.withTransaction
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.*
import org.koitharu.kotatsu.base.domain.ReversibleHandle
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.db.entity.*
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.favourites.data.FavouriteCategoryEntity
import org.koitharu.kotatsu.favourites.data.FavouriteEntity
import org.koitharu.kotatsu.favourites.data.toFavouriteCategory
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.SortOrder
import org.koitharu.kotatsu.tracker.work.TrackerNotificationChannels
import org.koitharu.kotatsu.utils.ext.mapItems

@Singleton
class FavouritesRepository @Inject constructor(
	private val db: MangaDatabase,
	private val channels: TrackerNotificationChannels,
) {

	suspend fun getAllManga(): List<Manga> {
		val entities = db.favouritesDao.findAll()
		return entities.map { it.manga.toManga(it.tags.toMangaTags()) }
	}

	fun observeAll(order: SortOrder): Flow<List<Manga>> {
		return db.favouritesDao.observeAll(order)
			.mapItems { it.manga.toManga(it.tags.toMangaTags()) }
	}

	suspend fun getManga(categoryId: Long): List<Manga> {
		val entities = db.favouritesDao.findAll(categoryId)
		return entities.map { it.manga.toManga(it.tags.toMangaTags()) }
	}

	fun observeAll(categoryId: Long, order: SortOrder): Flow<List<Manga>> {
		return db.favouritesDao.observeAll(categoryId, order)
			.mapItems { it.manga.toManga(it.tags.toMangaTags()) }
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

	fun observeCategoriesWithCovers(): Flow<Map<FavouriteCategory, List<String>>> {
		return db.favouriteCategoriesDao.observeAll()
			.map {
				db.withTransaction {
					val res = LinkedHashMap<FavouriteCategory, List<String>>()
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

	suspend fun createCategory(title: String, sortOrder: SortOrder, isTrackerEnabled: Boolean): FavouriteCategory {
		val entity = FavouriteCategoryEntity(
			title = title,
			createdAt = System.currentTimeMillis(),
			sortKey = db.favouriteCategoriesDao.getNextSortKey(),
			categoryId = 0,
			order = sortOrder.name,
			track = isTrackerEnabled,
			deletedAt = 0L,
			isVisibleInLibrary = true,
		)
		val id = db.favouriteCategoriesDao.insert(entity)
		val category = entity.toFavouriteCategory(id)
		channels.createChannel(category)
		return category
	}

	suspend fun updateCategory(id: Long, title: String, sortOrder: SortOrder, isTrackerEnabled: Boolean) {
		db.favouriteCategoriesDao.update(id, title, sortOrder.name, isTrackerEnabled)
	}

	suspend fun updateCategory(id: Long, isVisibleInLibrary: Boolean) {
		db.favouriteCategoriesDao.updateLibVisibility(id, isVisibleInLibrary)
	}

	suspend fun removeCategory(id: Long) {
		db.withTransaction {
			db.favouriteCategoriesDao.delete(id)
			db.favouritesDao.deleteAll(id)
		}
		channels.deleteChannel(id)
	}

	suspend fun removeCategories(ids: Collection<Long>) {
		db.withTransaction {
			for (id in ids) {
				db.favouriteCategoriesDao.delete(id)
				db.favouritesDao.deleteAll(id)
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
