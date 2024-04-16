package org.koitharu.kotatsu.tracker.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import org.koitharu.kotatsu.core.db.entity.MangaEntity

@Entity(
	tableName = "track_logs",
	foreignKeys = [
		ForeignKey(
			entity = MangaEntity::class,
			parentColumns = ["manga_id"],
			childColumns = ["manga_id"],
			onDelete = ForeignKey.CASCADE,
		),
	],
)
class TrackLogEntity(
	@PrimaryKey(autoGenerate = true)
	@ColumnInfo(name = "id") val id: Long = 0L,
	@ColumnInfo(name = "manga_id", index = true) val mangaId: Long,
	@ColumnInfo(name = "chapters") val chapters: String,
	@ColumnInfo(name = "created_at") val createdAt: Long,
	@ColumnInfo(name = "unread") val isUnread: Boolean,
)
