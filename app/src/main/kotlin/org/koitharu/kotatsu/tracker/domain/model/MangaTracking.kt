package org.koitharu.kotatsu.tracker.domain.model

import java.util.*
import org.koitharu.kotatsu.parsers.model.Manga

class MangaTracking(
	val manga: Manga,
	val lastChapterId: Long,
	val lastCheck: Date?,
) {

	fun isEmpty(): Boolean {
		return lastChapterId == 0L
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as MangaTracking

		if (manga != other.manga) return false
		if (lastChapterId != other.lastChapterId) return false
		if (lastCheck != other.lastCheck) return false

		return true
	}

	override fun hashCode(): Int {
		var result = manga.hashCode()
		result = 31 * result + lastChapterId.hashCode()
		result = 31 * result + (lastCheck?.hashCode() ?: 0)
		return result
	}
}