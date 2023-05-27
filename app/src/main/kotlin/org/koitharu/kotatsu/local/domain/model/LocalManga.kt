package org.koitharu.kotatsu.local.domain.model

import androidx.core.net.toFile
import androidx.core.net.toUri
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaTag
import java.io.File

class LocalManga(
	val file: File,
	val manga: Manga,
) {

	constructor(manga: Manga) : this(manga.url.toUri().toFile(), manga)

	var createdAt: Long = -1L
		private set
		get() {
			if (field == -1L) {
				field = file.lastModified()
			}
			return field
		}

	fun isMatchesQuery(query: String): Boolean {
		return manga.title.contains(query, ignoreCase = true) ||
			manga.altTitle?.contains(query, ignoreCase = true) == true
	}

	fun containsTags(tags: Set<MangaTag>): Boolean {
		return manga.tags.containsAll(tags)
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as LocalManga

		if (manga != other.manga) return false
		return file == other.file
	}

	override fun hashCode(): Int {
		var result = manga.hashCode()
		result = 31 * result + file.hashCode()
		return result
	}

	override fun toString(): String {
		return "LocalManga(${file.path}: ${manga.title})"
	}
}
