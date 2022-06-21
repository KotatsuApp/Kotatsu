package org.koitharu.kotatsu.tracker.domain.model

import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter

class MangaUpdates(
	val manga: Manga,
	val newChapters: List<MangaChapter>,
	val isValid: Boolean,
) {

	fun isNotEmpty() = newChapters.isNotEmpty()
}