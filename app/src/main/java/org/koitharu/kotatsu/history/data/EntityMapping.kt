package org.koitharu.kotatsu.history.data

import java.util.*
import org.koitharu.kotatsu.core.model.MangaHistory

fun HistoryEntity.toMangaHistory() = MangaHistory(
	createdAt = Date(createdAt),
	updatedAt = Date(updatedAt),
	chapterId = chapterId,
	page = page,
	scroll = scroll.toInt()
)