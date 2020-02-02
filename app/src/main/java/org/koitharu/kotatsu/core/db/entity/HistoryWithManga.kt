package org.koitharu.kotatsu.core.db.entity

import androidx.room.Embedded
import androidx.room.Relation

data class HistoryWithManga(
	@Embedded val history: HistoryEntity,
	@Relation(
		parentColumn = "manga_id",
		entityColumn = "manga_id"
	)
	val manga: MangaEntity
)