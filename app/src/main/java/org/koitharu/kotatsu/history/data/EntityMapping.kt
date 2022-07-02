package org.koitharu.kotatsu.history.data

import org.koitharu.kotatsu.core.model.MangaHistory
import java.util.*

fun HistoryEntity.toMangaHistory() = MangaHistory(
	createdAt = Date(createdAt),
	updatedAt = Date(updatedAt),
	chapterId = chapterId,
	page = page,
	scroll = scroll.toInt(),
	percent = percent,
)