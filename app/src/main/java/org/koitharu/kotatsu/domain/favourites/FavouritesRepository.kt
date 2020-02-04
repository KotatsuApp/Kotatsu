package org.koitharu.kotatsu.domain.favourites

import org.koin.core.KoinComponent
import org.koin.core.inject
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.db.entity.FavouriteCategoryEntity
import org.koitharu.kotatsu.core.db.entity.FavouriteEntity
import org.koitharu.kotatsu.core.db.entity.MangaEntity
import org.koitharu.kotatsu.core.db.entity.TagEntity
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.core.model.Manga

class FavouritesRepository : KoinComponent {

	private val db: MangaDatabase by inject()

	suspend fun getAllManga(offset: Int): List<Manga> {
		val entities = db.favouritesDao().findAll(offset, 20, "created_at")
		return entities.map { it.manga.toManga(it.tags.map(TagEntity::toMangaTag).toSet()) }
	}

	suspend fun getAllCategories(): List<FavouriteCategory> {
		val entities = db.favouriteCategoriesDao().findAll("created_at")
		return entities.map { it.toFavouriteCategory() }
	}

	suspend fun getCategories(mangaId: Long): List<FavouriteCategory> {
		val entities = db.favouritesDao().find(mangaId)?.categories
		return entities?.map { it.toFavouriteCategory() }.orEmpty()
	}

	suspend fun addCategory(title: String): FavouriteCategory {
		val entity = FavouriteCategoryEntity(
			title = title,
			createdAt = System.currentTimeMillis(),
			categoryId = 0
		)
		val id = db.favouriteCategoriesDao().insert(entity)
		return entity.toFavouriteCategory(id)
	}

	suspend fun removeCategory(id: Long) {
		db.favouriteCategoriesDao().delete(id)
	}

	suspend fun addToCategory(manga: Manga, categoryId: Long) {
		val tags = manga.tags.map(TagEntity.Companion::fromMangaTag)
		db.tagsDao().upsert(tags)
		db.mangaDao().upsert(MangaEntity.from(manga), tags)
		val entity = FavouriteEntity(manga.id, categoryId, System.currentTimeMillis())
		db.favouritesDao().add(entity)
	}

	suspend fun removeFromCategory(manga: Manga, categoryId: Long) {
		db.favouritesDao().delete(categoryId, manga.id)
	}
}