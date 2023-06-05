package org.koitharu.kotatsu.favourites.domain.model

import org.koitharu.kotatsu.parsers.model.MangaSource

class Cover(
	val url: String,
	val source: String,
) {

	val mangaSource: MangaSource?
		get() = if (source.isEmpty()) null else MangaSource.values().find { it.name == source }

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as Cover

		if (url != other.url) return false
		return source == other.source
	}

	override fun hashCode(): Int {
		var result = url.hashCode()
		result = 31 * result + source.hashCode()
		return result
	}

	override fun toString(): String {
		return "Cover(url='$url', source=$source)"
	}
}
