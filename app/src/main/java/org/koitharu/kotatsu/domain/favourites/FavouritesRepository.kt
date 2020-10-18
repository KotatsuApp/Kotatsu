package org.koitharu.kotatsu.domain.favourites

import androidx.collection.ArraySet
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.db.entity.FavouriteCategoryEntity
import org.koitharu.kotatsu.core.db.entity.FavouriteEntity
import org.koitharu.kotatsu.core.db.entity.MangaEntity
import org.koitharu.kotatsu.core.db.entity.TagEntity
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.core.model.Manga

class FavouritesRepository(private val db: MangaDatabase) {

	suspend fun getAllManga(): List<Manga> {
		val entities = db.favouritesDao.findAll()
		return entities.map { it.manga.toManga(it.tags.map(TagEntity::toMangaTag).toSet()) }
	}

	suspend fun getAllManga(offset: Int): List<Manga> {
		val entities = db.favouritesDao.findAll(offset, 20)
		return entities.map { it.manga.toManga(it.tags.map(TagEntity::toMangaTag).toSet()) }
	}

	suspend fun getManga(categoryId: Long): List<Manga> {
		val entities = db.favouritesDao.findAll(categoryId)
		return entities.map { it.manga.toManga(it.tags.map(TagEntity::toMangaTag).toSet()) }
	}

	suspend fun getManga(categoryId: Long, offset: Int): List<Manga> {
		val entities = db.favouritesDao.findAll(categoryId, offset, 20)
		return entities.map { it.manga.toManga(it.tags.map(TagEntity::toMangaTag).toSet()) }
	}

	suspend fun getAllCategories(): List<FavouriteCategory> {
		val entities = db.favouriteCategoriesDao.findAll()
		return entities.map { it.toFavouriteCategory() }
	}

	suspend fun getCategories(mangaId: Long): List<FavouriteCategory> {
		val entities = db.favouritesDao.find(mangaId)?.categories
		return entities?.map { it.toFavouriteCategory() }.orEmpty()
	}

	suspend fun addCategory(title: String): FavouriteCategory {
		val entity = FavouriteCategoryEntity(
			title = title,
			createdAt = System.currentTimeMillis(),
			sortKey = db.favouriteCategoriesDao.getNextSortKey(),
			categoryId = 0
		)
		val id = db.favouriteCategoriesDao.insert(entity)
		notifyCategoriesChanged()
		return entity.toFavouriteCategory(id)
	}

	suspend fun renameCategory(id: Long, title: String) {
		db.favouriteCategoriesDao.update(id, title)
		notifyCategoriesChanged()
	}

	suspend fun removeCategory(id: Long) {
		db.favouriteCategoriesDao.delete(id)
		notifyCategoriesChanged()
	}

	suspend fun reorderCategories(orderedIds: List<Long>) {
		val dao = db.favouriteCategoriesDao
		db.withTransaction {
			for ((i, id) in orderedIds.withIndex()) {
				dao.update(id, i)
			}
		}
		notifyCategoriesChanged()
	}

	suspend fun addToCategory(manga: Manga, categoryId: Long) {
		val tags = manga.tags.map(TagEntity.Companion::fromMangaTag)
		db.withTransaction {
			db.tagsDao.upsert(tags)
			db.mangaDao.upsert(MangaEntity.from(manga), tags)
			val entity = FavouriteEntity(manga.id, categoryId, System.currentTimeMillis())
			db.favouritesDao.add(entity)
		}
		notifyFavouritesChanged(manga.id)
	}

	suspend fun removeFromCategory(manga: Manga, categoryId: Long) {
		db.favouritesDao.delete(categoryId, manga.id)
		notifyFavouritesChanged(manga.id)
	}

	companion object {

		private val listeners = ArraySet<OnFavouritesChangeListener>()

		fun subscribe(listener: OnFavouritesChangeListener) {
			listeners += listener
		}

		fun unsubscribe(listener: OnFavouritesChangeListener) {
			listeners -= listener
		}

		private suspend fun notifyFavouritesChanged(mangaId: Long) {
			withContext(Dispatchers.Main) {
				listeners.forEach { x -> x.onFavouritesChanged(mangaId) }
			}
		}

		private suspend fun notifyCategoriesChanged() {
			withContext(Dispatchers.Main) {
				listeners.forEach { x -> x.onCategoriesChanged() }
			}
		}
	}
}