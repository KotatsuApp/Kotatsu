package org.koitharu.kotatsu.local.domain

import java.io.File
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaTag

class LocalManga(
	val manga: Manga,
	val file: File,
) {

	var createdAt: Long = -1L
		get() {
			if (field == -1L) {
				field = file.lastModified()
			}
			return field
		}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as LocalManga

		if (manga != other.manga) return false
		if (file != other.file) return false

		return true
	}

	override fun hashCode(): Int {
		var result = manga.hashCode()
		result = 31 * result + file.hashCode()
		return result
	}
}

fun Collection<LocalManga>.unwrap(): List<Manga> = map { it.manga }

fun LocalManga.isMatchesQuery(query: String): Boolean {
	return manga.title.contains(query, ignoreCase = true) ||
		manga.altTitle?.contains(query, ignoreCase = true) == true
}

fun LocalManga.containsTags(tags: Set<MangaTag>): Boolean {
	return manga.tags.containsAll(tags)
}
