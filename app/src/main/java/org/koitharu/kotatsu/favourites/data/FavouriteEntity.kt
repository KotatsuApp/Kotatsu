package org.koitharu.kotatsu.favourites.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import org.koitharu.kotatsu.core.db.entity.MangaEntity

@Entity(
	tableName = "favourites", primaryKeys = ["manga_id", "category_id"], foreignKeys = [
		ForeignKey(
			entity = MangaEntity::class,
			parentColumns = ["manga_id"],
			childColumns = ["manga_id"],
			onDelete = ForeignKey.CASCADE
		),
		ForeignKey(
			entity = FavouriteCategoryEntity::class,
			parentColumns = ["category_id"],
			childColumns = ["category_id"],
			onDelete = ForeignKey.CASCADE
		)
	]
)
class FavouriteEntity(
	@ColumnInfo(name = "manga_id", index = true) val mangaId: Long,
	@ColumnInfo(name = "category_id", index = true) val categoryId: Long,
	@ColumnInfo(name = "sort_key") val sortKey: Int,
	@ColumnInfo(name = "created_at") val createdAt: Long,
) {

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as FavouriteEntity

		if (mangaId != other.mangaId) return false
		if (categoryId != other.categoryId) return false
		if (sortKey != other.sortKey) return false
		if (createdAt != other.createdAt) return false

		return true
	}

	override fun hashCode(): Int {
		var result = mangaId.hashCode()
		result = 31 * result + categoryId.hashCode()
		result = 31 * result + sortKey
		result = 31 * result + createdAt.hashCode()
		return result
	}
}