package org.koitharu.kotatsu.backups.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koitharu.kotatsu.stats.data.StatsEntity

@Serializable
class StatisticBackup(
	@SerialName("manga_id") val mangaId: Long,
	@SerialName("started_at") val startedAt: Long,
	@SerialName("duration") val duration: Long,
	@SerialName("pages") val pages: Int,
) {

	constructor(entity: StatsEntity) : this(
		mangaId = entity.mangaId,
		startedAt = entity.startedAt,
		duration = entity.duration,
		pages = entity.pages,
	)

	fun toEntity() = StatsEntity(
		mangaId = mangaId,
		startedAt = startedAt,
		duration = duration,
		pages = pages,
	)
}
