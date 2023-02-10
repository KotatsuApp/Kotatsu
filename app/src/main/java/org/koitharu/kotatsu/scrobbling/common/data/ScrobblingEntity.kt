package org.koitharu.kotatsu.scrobbling.common.data

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
	tableName = "scrobblings",
	primaryKeys = ["scrobbler", "id", "manga_id"],
)
class ScrobblingEntity(
	@ColumnInfo(name = "scrobbler") val scrobbler: Int,
	@ColumnInfo(name = "id") val id: Int,
	@ColumnInfo(name = "manga_id") val mangaId: Long,
	@ColumnInfo(name = "target_id") val targetId: Long,
	@ColumnInfo(name = "status") val status: String?,
	@ColumnInfo(name = "chapter") val chapter: Int,
	@ColumnInfo(name = "comment") val comment: String?,
	@ColumnInfo(name = "rating") val rating: Float,
) {

	fun copy(
		status: String?,
		comment: String?,
		rating: Float,
	) = ScrobblingEntity(
		scrobbler = scrobbler,
		id = id,
		mangaId = mangaId,
		targetId = targetId,
		status = status,
		chapter = chapter,
		comment = comment,
		rating = rating,
	)
}
