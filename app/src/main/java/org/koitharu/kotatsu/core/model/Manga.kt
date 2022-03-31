package org.koitharu.kotatsu.core.model

import org.koitharu.kotatsu.parsers.model.Manga

fun Manga.withoutChapters() = if (chapters.isNullOrEmpty()) {
	this
} else {
	Manga(
		id = id,
		title = title,
		altTitle = altTitle,
		url = url,
		publicUrl = publicUrl,
		rating = rating,
		isNsfw = isNsfw,
		coverUrl = coverUrl,
		tags = tags,
		state = state,
		author = author,
		largeCoverUrl = largeCoverUrl,
		description = description,
		chapters = null,
		source = source,
	)
}