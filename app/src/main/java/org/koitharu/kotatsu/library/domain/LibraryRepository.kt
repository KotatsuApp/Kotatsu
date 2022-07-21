package org.koitharu.kotatsu.library.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.db.entity.toManga
import org.koitharu.kotatsu.core.db.entity.toMangaTags
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.favourites.data.toFavouriteCategory
import org.koitharu.kotatsu.parsers.model.Manga

class LibraryRepository(
	private val db: MangaDatabase,
) {

	fun observeFavourites(): Flow<Map<FavouriteCategory, List<Manga>>> {
		return db.favouriteCategoriesDao.observeAll()
			.flatMapLatest { categories ->
				combine(
					categories.map { cat ->
						val category = cat.toFavouriteCategory()
						db.favouritesDao.observeAll(category.id, category.order)
							.map { category to it.map { x -> x.manga.toManga(x.tags.toMangaTags()) } }
					},
				) { array -> array.toMap() }
			}
	}
}
