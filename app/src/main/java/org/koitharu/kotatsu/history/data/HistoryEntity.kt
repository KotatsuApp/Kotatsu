package org.koitharu.kotatsu.history.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import org.koitharu.kotatsu.core.db.entity.MangaEntity
import org.koitharu.kotatsu.core.model.MangaHistory
import java.util.*

@Entity(
	tableName = "history", foreignKeys = [
		ForeignKey(
			entity = MangaEntity::class,
			parentColumns = ["manga_id"],
			childColumns = ["manga_id"],
			onDelete = ForeignKey.CASCADE
		)
	]
)
data class HistoryEntity(
	@PrimaryKey(autoGenerate = false)
	@ColumnInfo(name = "manga_id") val mangaId: Long,
	@ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
	@ColumnInfo(name = "updated_at") val updatedAt: Long,
	@ColumnInfo(name = "chapter_id") val chapterId: Long,
	@ColumnInfo(name = "page") val page: Int,
	@ColumnInfo(name = "scroll") val scroll: Float
) {

	fun toMangaHistory() = MangaHistory(
		createdAt = Date(createdAt),
		updatedAt = Date(updatedAt),
		chapterId = chapterId,
		page = page,
		scroll = scroll.toInt()
	)
}