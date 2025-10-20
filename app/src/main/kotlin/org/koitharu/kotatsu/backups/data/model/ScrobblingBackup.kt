package org.koitharu.kotatsu.backups.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koitharu.kotatsu.scrobbling.common.data.ScrobblingEntity

@Serializable
class ScrobblingBackup(
	@SerialName("scrobbler") val scrobbler: Int,
	@SerialName("id") val id: Int,
	@SerialName("manga_id") val mangaId: Long,
	@SerialName("target_id") val targetId: Long,
	@SerialName("status") val status: String?,
	@SerialName("chapter") val chapter: Int,
	@SerialName("comment") val comment: String?,
	@SerialName("rating") val rating: Float,
) {

	constructor(entity: ScrobblingEntity) : this(
		scrobbler = entity.scrobbler,
		id = entity.id,
		mangaId = entity.mangaId,
		targetId = entity.targetId,
		status = entity.status,
		chapter = entity.chapter,
		comment = entity.comment,
		rating = entity.rating,
	)

	fun toEntity() = ScrobblingEntity(
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
