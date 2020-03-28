package org.koitharu.kotatsu.core.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
	tableName = "manga_tags", primaryKeys = ["manga_id", "tag_id"], foreignKeys = [
		ForeignKey(
			entity = MangaEntity::class,
			parentColumns = ["manga_id"],
			childColumns = ["manga_id"],
			onDelete = ForeignKey.CASCADE
		),
		ForeignKey(
			entity = TagEntity::class,
			parentColumns = ["tag_id"],
			childColumns = ["tag_id"],
			onDelete = ForeignKey.CASCADE
		)
	]
)
data class MangaTagsEntity(
	@ColumnInfo(name = "manga_id", index = true) val mangaId: Long,
	@ColumnInfo(name = "tag_id", index = true) val tagId: Long
)