package org.koitharu.kotatsu.favourites.domain

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.db.entity.*
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.favourites.data.FavouriteCategoryEntity
import org.koitharu.kotatsu.favourites.data.FavouriteEntity
import org.koitharu.kotatsu.favourites.data.toFavouriteCategory
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.SortOrder
import org.koitharu.kotatsu.utils.ext.mapItems

class FavouritesRepository(private val db: MangaDatabase) {

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

	fun observeCategories(mangaId: Long): Flow<List<FavouriteCategory>> {
		return db.favouritesDao.observe(mangaId).map { entity ->
			entity?.categories?.map { it.toFavouriteCategory() }.orEmpty()
		}.distinctUntilChanged()
	}

	fun observeCategoriesIds(mangaId: Long): Flow<Set<Long>> {
		return db.favouritesDao.observeIds(mangaId).map { it.toSet() }
	}

	suspend fun addCategory(title: String): FavouriteCategory {
		val entity = FavouriteCategoryEntity(
			title = title,
			createdAt = System.currentTimeMillis(),
			sortKey = db.favouriteCategoriesDao.getNextSortKey(),
			categoryId = 0,
			order = SortOrder.NEWEST.name,
		)
		val id = db.favouriteCategoriesDao.insert(entity)
		return entity.toFavouriteCategory(id)
	}

	suspend fun renameCategory(id: Long, title: String) {
		db.favouriteCategoriesDao.updateTitle(id, title)
	}

	suspend fun removeCategory(id: Long) {
		db.favouriteCategoriesDao.delete(id)
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
				val entity = FavouriteEntity(manga.id, categoryId, System.currentTimeMillis())
				db.favouritesDao.insert(entity)
			}
		}
	}

	suspend fun removeFromFavourites(ids: Collection<Long>) {
		db.withTransaction {
			for (id in ids) {
				db.favouritesDao.delete(id)
			}
		}
	}

	suspend fun removeFromCategory(categoryId: Long, ids: Collection<Long>) {
		db.withTransaction {
			for (id in ids) {
				db.favouritesDao.delete(categoryId, id)
			}
		}
	}

	private fun observeOrder(categoryId: Long): Flow<SortOrder> {
		return db.favouriteCategoriesDao.observe(categoryId)
			.map { x -> SortOrder(x.order, SortOrder.NEWEST) }
			.distinctUntilChanged()
	}
}