package org.koitharu.kotatsu.core.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import org.koitharu.kotatsu.core.db.TABLE_CHAPTERS

@Entity(
	tableName = TABLE_CHAPTERS,
	primaryKeys = ["manga_id", "chapter_id"],
	foreignKeys = [
		ForeignKey(
			entity = MangaEntity::class,
			parentColumns = ["manga_id"],
			childColumns = ["manga_id"],
			onDelete = ForeignKey.CASCADE,
		),
	],
)
data class ChapterEntity(
	@ColumnInfo(name = "chapter_id") val chapterId: Long,
	@ColumnInfo(name = "manga_id") val mangaId: Long,
	@ColumnInfo(name = "name") val title: String,
	@ColumnInfo(name = "number") val number: Float,
	@ColumnInfo(name = "volume") val volume: Int,
	@ColumnInfo(name = "url") val url: String,
	@ColumnInfo(name = "scanlator") val scanlator: String?,
	@ColumnInfo(name = "upload_date") val uploadDate: Long,
	@ColumnInfo(name = "branch") val branch: String?,
	@ColumnInfo(name = "source") val source: String,
	@ColumnInfo(name = "index") val index: Int,
)
