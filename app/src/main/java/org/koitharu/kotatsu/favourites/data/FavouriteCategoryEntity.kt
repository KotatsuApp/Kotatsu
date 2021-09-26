package org.koitharu.kotatsu.favourites.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.core.model.SortOrder
import java.util.*

@Entity(tableName = "favourite_categories")
data class FavouriteCategoryEntity(
	@PrimaryKey(autoGenerate = true)
	@ColumnInfo(name = "category_id") val categoryId: Int,
	@ColumnInfo(name = "created_at") val createdAt: Long,
	@ColumnInfo(name = "sort_key") val sortKey: Int,
	@ColumnInfo(name = "title") val title: String,
	@ColumnInfo(name = "order") val order: String,
) {

	fun toFavouriteCategory(id: Long? = null) = FavouriteCategory(
		id = id ?: categoryId.toLong(),
		title = title,
		sortKey = sortKey,
		order = SortOrder.values().find { x -> x.name == order } ?: SortOrder.NEWEST,
		createdAt = Date(createdAt),
	)
}