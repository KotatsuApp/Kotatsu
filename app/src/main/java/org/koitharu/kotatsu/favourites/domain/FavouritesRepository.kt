package org.koitharu.kotatsu.favourites.domain

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.db.entity.MangaEntity
import org.koitharu.kotatsu.core.db.entity.TagEntity
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.favourites.data.FavouriteCategoryEntity
import org.koitharu.kotatsu.favourites.data.FavouriteEntity
import org.koitharu.kotatsu.utils.ext.mapItems
import org.koitharu.kotatsu.utils.ext.mapToSet

class FavouritesRepository(private val db: MangaDatabase) {

	suspend fun getAllManga(): List<Manga> {
		val entities = db.favouritesDao.findAll()
		return entities.map { it.manga.toManga(it.tags.mapToSet(TagEntity::toMangaTag)) }
	}

	fun observeAll(): Flow<List<Manga>> {
		return db.favouritesDao.observeAll()
			.mapItems { it.manga.toManga(it.tags.mapToSet(TagEntity::toMangaTag)) }
	}

	suspend fun getAllManga(offset: Int): List<Manga> {
		val entities = db.favouritesDao.findAll(offset, 20)
		return entities.map { it.manga.toManga(it.tags.mapToSet(TagEntity::toMangaTag)) }
	}

	suspend fun getManga(categoryId: Long): List<Manga> {
		val entities = db.favouritesDao.findAll(categoryId)
		return entities.map { it.manga.toManga(it.tags.mapToSet(TagEntity::toMangaTag)) }
	}

	fun observeAll(categoryId: Long): Flow<List<Manga>> {
		return db.favouritesDao.observeAll(categoryId)
			.mapItems { it.manga.toManga(it.tags.mapToSet(TagEntity::toMangaTag)) }
	}

	suspend fun getManga(categoryId: Long, offset: Int): List<Manga> {
		val entities = db.favouritesDao.findAll(categoryId, offset, 20)
		return entities.map { it.manga.toManga(it.tags.mapToSet(TagEntity::toMangaTag)) }
	}

	suspend fun getAllCategories(): List<FavouriteCategory> {
		val entities = db.favouriteCategoriesDao.findAll()
		return entities.map { it.toFavouriteCategory() }
	}

	suspend fun getCategories(mangaId: Long): List<FavouriteCategory> {
		val entities = db.favouritesDao.find(mangaId)?.categories
		return entities?.map { it.toFavouriteCategory() }.orEmpty()
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

	fun observeCategoriesIds(mangaId: Long): Flow<List<Long>> {
		return db.favouritesDao.observeIds(mangaId)
	}

	suspend fun addCategory(title: String): FavouriteCategory {
		val entity = FavouriteCategoryEntity(
			title = title,
			createdAt = System.currentTimeMillis(),
			sortKey = db.favouriteCategoriesDao.getNextSortKey(),
			categoryId = 0
		)
		val id = db.favouriteCategoriesDao.insert(entity)
		return entity.toFavouriteCategory(id)
	}

	suspend fun renameCategory(id: Long, title: String) {
		db.favouriteCategoriesDao.update(id, title)
	}

	suspend fun removeCategory(id: Long) {
		db.favouriteCategoriesDao.delete(id)
	}

	suspend fun reorderCategories(orderedIds: List<Long>) {
		val dao = db.favouriteCategoriesDao
		db.withTransaction {
			for ((i, id) in orderedIds.withIndex()) {
				dao.update(id, i)
			}
		}
	}

	suspend fun addToCategory(manga: Manga, categoryId: Long) {
		val tags = manga.tags.map(TagEntity.Companion::fromMangaTag)
		db.withTransaction {
			db.tagsDao.upsert(tags)
			db.mangaDao.upsert(MangaEntity.from(manga), tags)
			val entity = FavouriteEntity(manga.id, categoryId, System.currentTimeMillis())
			db.favouritesDao.insert(entity)
		}
	}

	suspend fun removeFromCategory(manga: Manga, categoryId: Long) {
		db.favouritesDao.delete(categoryId, manga.id)
	}

	suspend fun removeFromFavourites(manga: Manga) {
		db.favouritesDao.delete(manga.id)
	}
}