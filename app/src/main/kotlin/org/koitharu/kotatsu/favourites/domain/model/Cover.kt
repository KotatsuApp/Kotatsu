package org.koitharu.kotatsu.favourites.domain.model

import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.util.find

data class Cover(
	val url: String,
	val source: String,
) {
	val mangaSource: MangaSource?
		get() = if (source.isEmpty()) null else MangaSource.entries.find(source)
}
