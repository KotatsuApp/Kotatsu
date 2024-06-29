package org.koitharu.kotatsu.favourites.domain.model

import org.koitharu.kotatsu.core.model.MangaSource

data class Cover(
	val url: String,
	val source: String,
) {
	val mangaSource by lazy { MangaSource(source) }
}
