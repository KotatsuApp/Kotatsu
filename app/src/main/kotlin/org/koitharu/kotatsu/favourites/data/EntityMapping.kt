package org.koitharu.kotatsu.favourites.data

import org.koitharu.kotatsu.core.db.entity.SortOrder
import org.koitharu.kotatsu.core.db.entity.toManga
import org.koitharu.kotatsu.core.db.entity.toMangaTags
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.parsers.model.SortOrder
import java.util.Date

fun FavouriteCategoryEntity.toFavouriteCategory(id: Long = categoryId.toLong()) = FavouriteCategory(
	id = id,
	title = title,
	sortKey = sortKey,
	order = SortOrder(order, SortOrder.NEWEST),
	createdAt = Date(createdAt),
	isTrackingEnabled = track,
	isVisibleInLibrary = isVisibleInLibrary,
)

fun FavouriteManga.toManga() = manga.toManga(tags.toMangaTags())

fun Collection<FavouriteManga>.toMangaList() = map { it.toManga() }
