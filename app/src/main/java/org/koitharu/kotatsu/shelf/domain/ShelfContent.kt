package org.koitharu.kotatsu.shelf.domain

import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.history.domain.MangaWithHistory
import org.koitharu.kotatsu.parsers.model.Manga

class ShelfContent(
	val history: List<MangaWithHistory>,
	val favourites: Map<FavouriteCategory, List<Manga>>,
	val updated: Map<Manga, Int>,
	val local: List<Manga>,
	val suggestions: List<Manga>,
) {

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as ShelfContent

		if (history != other.history) return false
		if (favourites != other.favourites) return false
		if (updated != other.updated) return false
		if (local != other.local) return false
		return suggestions == other.suggestions
	}

	override fun hashCode(): Int {
		var result = history.hashCode()
		result = 31 * result + favourites.hashCode()
		result = 31 * result + updated.hashCode()
		result = 31 * result + local.hashCode()
		result = 31 * result + suggestions.hashCode()
		return result
	}
}
