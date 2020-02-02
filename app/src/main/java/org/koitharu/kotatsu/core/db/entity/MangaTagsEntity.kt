package org.koitharu.kotatsu.core.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "manga_tags", primaryKeys = ["manga_id", "tag_id"])
data class MangaTagsEntity(
	@ColumnInfo(name = "manga_id") val mangaId: Long,
	@ColumnInfo(name = "tag_id") val tagId: Long
)