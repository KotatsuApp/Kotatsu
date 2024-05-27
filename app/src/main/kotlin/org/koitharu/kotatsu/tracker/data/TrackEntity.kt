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
	@ColumnInfo(name = "last_chapter_id") val lastChapterId: Long,
	@ColumnInfo(name = "chapters_new") val newChapters: Int,
	@ColumnInfo(name = "last_check_time") val lastCheckTime: Long,
	@ColumnInfo(name = "last_chapter_date") val lastChapterDate: Long,
	@ColumnInfo(name = "last_result") val lastResult: Int,
	@ColumnInfo(name = "last_error") val lastError: String?,
) {

	companion object {

		const val RESULT_NONE = 0
		const val RESULT_HAS_UPDATE = 1
		const val RESULT_NO_UPDATE = 2
		const val RESULT_FAILED = 3
		const val RESULT_EXTERNAL_MODIFICATION = 4

		fun create(mangaId: Long) = TrackEntity(
			mangaId = mangaId,
			lastChapterId = 0L,
			newChapters = 0,
			lastCheckTime = 0L,
			lastChapterDate = 0,
			lastResult = RESULT_NONE,
			lastError = null,
		)
	}
}
