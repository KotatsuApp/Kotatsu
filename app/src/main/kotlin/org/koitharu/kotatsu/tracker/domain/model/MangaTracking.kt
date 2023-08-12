package org.koitharu.kotatsu.tracker.domain.model

import org.koitharu.kotatsu.parsers.model.Manga
import java.time.Instant

data class MangaTracking(
	val manga: Manga,
	val lastChapterId: Long,
	val lastCheck: Instant?,
) {
	fun isEmpty(): Boolean {
		return lastChapterId == 0L
	}
}
