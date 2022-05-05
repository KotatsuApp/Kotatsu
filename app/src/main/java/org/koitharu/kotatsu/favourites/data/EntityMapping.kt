package org.koitharu.kotatsu.favourites.data

import java.util.*
import org.koitharu.kotatsu.core.db.entity.SortOrder
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.parsers.model.SortOrder

fun FavouriteCategoryEntity.toFavouriteCategory(id: Long = categoryId.toLong()) = FavouriteCategory(
	id = id,
	title = title,
	sortKey = sortKey,
	order = SortOrder(order, SortOrder.NEWEST),
	createdAt = Date(createdAt),
	isTrackingEnabled = track,
)