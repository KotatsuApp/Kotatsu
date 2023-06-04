package org.koitharu.kotatsu.shelf.domain.model

import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.parsers.model.Manga

class ShelfContent(
	val history: List<Manga>,
	val favourites: Map<FavouriteCategory, List<Manga>>,
	val updated: List<Manga>,
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
