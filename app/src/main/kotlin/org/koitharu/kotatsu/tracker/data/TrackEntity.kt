package org.koitharu.kotatsu.tracker.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import org.koitharu.kotatsu.core.db.entity.MangaEntity

@Entity(
	tableName = "tracks",
	foreignKeys = [
		ForeignKey(
			entity = MangaEntity::class,
			parentColumns = ["manga_id"],
			childColumns = ["manga_id"],
			onDelete = ForeignKey.CASCADE,
		),
	],
)
class TrackEntity(
	@PrimaryKey(autoGenerate = false)
	@ColumnInfo(name = "manga_id") val mangaId: Long,
	@get:Deprecated(message = "Should not be used", level = DeprecationLevel.WARNING)
	@ColumnInfo(name = "chapters_total") val totalChapters: Int,
	@ColumnInfo(name = "last_chapter_id") val lastChapterId: Long,
	@ColumnInfo(name = "chapters_new") val newChapters: Int,
	@ColumnInfo(name = "last_check") val lastCheck: Long,
	@get:Deprecated(message = "Should not be used", level = DeprecationLevel.WARNING)
	@ColumnInfo(name = "last_notified_id") val lastNotifiedChapterId: Long
)
