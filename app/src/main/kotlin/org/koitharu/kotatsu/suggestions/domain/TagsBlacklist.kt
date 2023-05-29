package org.koitharu.kotatsu.suggestions.domain

import org.koitharu.kotatsu.core.util.ext.almostEquals
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaTag

class TagsBlacklist(
	private val tags: Set<String>,
	private val threshold: Float,
) {

	fun isNotEmpty() = tags.isNotEmpty()

	operator fun contains(manga: Manga): Boolean {
		if (tags.isEmpty()) {
			return false
		}
		for (mangaTag in manga.tags) {
			for (tagTitle in tags) {
				if (mangaTag.title.almostEquals(tagTitle, threshold)) {
					return true
				}
			}
		}
		return false
	}

	operator fun contains(tag: MangaTag): Boolean = tags.any {
		it.almostEquals(tag.title, threshold)
	}
}
