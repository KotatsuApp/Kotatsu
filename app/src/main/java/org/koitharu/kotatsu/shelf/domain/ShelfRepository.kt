package org.koitharu.kotatsu.shelf.domain

import javax.inject.Inject
import kotlinx.coroutines.flow.*
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.db.entity.toManga
import org.koitharu.kotatsu.core.db.entity.toMangaTags
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.favourites.data.FavouriteCategoryEntity
import org.koitharu.kotatsu.favourites.data.toFavouriteCategory
import org.koitharu.kotatsu.parsers.model.Manga

class ShelfRepository @Inject constructor(
	private val db: MangaDatabase,
) {

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
