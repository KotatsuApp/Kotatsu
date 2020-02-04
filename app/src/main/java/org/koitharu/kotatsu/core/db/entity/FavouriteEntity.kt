package org.koitharu.kotatsu.core.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "favourites", primaryKeys = ["manga_id", "category_id"])
data class FavouriteEntity(
	@ColumnInfo(name = "manga_id") val mangaId: Long,
	@ColumnInfo(name = "category_id") val categoryId: Long,
	@ColumnInfo(name = "created_at") val createdAt: Long
)