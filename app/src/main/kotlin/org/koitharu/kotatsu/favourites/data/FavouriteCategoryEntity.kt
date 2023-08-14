package org.koitharu.kotatsu.favourites.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.koitharu.kotatsu.core.db.TABLE_FAVOURITE_CATEGORIES

@Entity(tableName = TABLE_FAVOURITE_CATEGORIES)
data class FavouriteCategoryEntity(
	@PrimaryKey(autoGenerate = true)
	@ColumnInfo(name = "category_id") val categoryId: Int,
	@ColumnInfo(name = "created_at") val createdAt: Long,
	@ColumnInfo(name = "sort_key") val sortKey: Int,
	@ColumnInfo(name = "title") val title: String,
	@ColumnInfo(name = "order") val order: String,
	@ColumnInfo(name = "track") val track: Boolean,
	@ColumnInfo(name = "show_in_lib") val isVisibleInLibrary: Boolean,
	@ColumnInfo(name = "deleted_at") val deletedAt: Long,
) {

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as FavouriteCategoryEntity

		if (categoryId != other.categoryId) return false
		if (createdAt != other.createdAt) return false
		if (sortKey != other.sortKey) return false
		if (title != other.title) return false
		if (order != other.order) return false
		if (track != other.track) return false
		return isVisibleInLibrary == other.isVisibleInLibrary
	}

	override fun hashCode(): Int {
		var result = categoryId
		result = 31 * result + createdAt.hashCode()
		result = 31 * result + sortKey
		result = 31 * result + title.hashCode()
		result = 31 * result + order.hashCode()
		result = 31 * result + track.hashCode()
		result = 31 * result + isVisibleInLibrary.hashCode()
		return result
	}
}
