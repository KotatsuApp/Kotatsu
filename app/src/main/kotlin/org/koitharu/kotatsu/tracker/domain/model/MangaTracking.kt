package org.koitharu.kotatsu.tracker.domain.model

import java.util.*
import org.koitharu.kotatsu.parsers.model.Manga

data class MangaTracking(
	val manga: Manga,
	val lastChapterId: Long,
	val lastCheck: Date?,
) {
	fun isEmpty(): Boolean {
		return lastChapterId == 0L
	}
}
