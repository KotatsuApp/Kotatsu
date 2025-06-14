package org.koitharu.kotatsu.backups.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koitharu.kotatsu.core.db.entity.MangaWithTags
import org.koitharu.kotatsu.history.data.HistoryEntity
import org.koitharu.kotatsu.history.data.HistoryWithManga
import org.koitharu.kotatsu.list.domain.ReadingProgress.Companion.PROGRESS_NONE

@Serializable
class HistoryBackup(
	@SerialName("manga_id") val mangaId: Long,
	@SerialName("created_at") val createdAt: Long,
	@SerialName("updated_at") val updatedAt: Long,
	@SerialName("chapter_id") val chapterId: Long,
	@SerialName("page") val page: Int,
	@SerialName("scroll") val scroll: Float,
	@SerialName("percent") val percent: Float = PROGRESS_NONE,
	@SerialName("chapters") val chaptersCount: Int = 0,
	@SerialName("manga") val manga: MangaBackup,
) {

	constructor(entity: HistoryWithManga) : this(
		mangaId = entity.manga.id,
		createdAt = entity.history.createdAt,
		updatedAt = entity.history.updatedAt,
		chapterId = entity.history.chapterId,
		page = entity.history.page,
		scroll = entity.history.scroll,
		percent = entity.history.percent,
		chaptersCount = entity.history.chaptersCount,
		manga = MangaBackup(MangaWithTags(entity.manga, entity.tags)),
	)

	fun toEntity() = HistoryEntity(
		mangaId = mangaId,
		createdAt = createdAt,
		updatedAt = updatedAt,
		chapterId = chapterId,
		page = page,
		scroll = scroll,
		percent = percent,
		deletedAt = 0L,
		chaptersCount = chaptersCount,
	)
}
