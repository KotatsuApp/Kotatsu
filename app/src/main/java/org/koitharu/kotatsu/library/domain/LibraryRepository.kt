package org.koitharu.kotatsu.library.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.db.entity.toManga
import org.koitharu.kotatsu.core.db.entity.toMangaTags
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.favourites.data.FavouriteManga
import org.koitharu.kotatsu.favourites.data.toFavouriteCategory
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository
import org.koitharu.kotatsu.history.domain.HistoryRepository
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.SortOrder

class LibraryRepository(
	private val db: MangaDatabase,
) {


	fun observeFavourites(order: SortOrder): Flow<Map<FavouriteCategory, List<Manga>>> {
		return db.favouritesDao.observeAll(order)
			.map { list -> groupByCategory(list) }
	}

	private fun groupByCategory(list: List<FavouriteManga>): Map<FavouriteCategory, List<Manga>> {
		val map = HashMap<FavouriteCategory, MutableList<Manga>>()
		for (item in list) {
			val manga = item.manga.toManga(item.tags.toMangaTags())
			for (category in item.categories) {
				if (!category.isVisibleInLibrary) {
					continue
				}
				map.getOrPut(category.toFavouriteCategory()) { ArrayList() }
					.add(manga)
			}
		}
		return map
	}
}