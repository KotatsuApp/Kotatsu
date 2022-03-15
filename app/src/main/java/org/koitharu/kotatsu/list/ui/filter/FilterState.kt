package org.koitharu.kotatsu.list.ui.filter

import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.parsers.model.SortOrder

class FilterState(
	val sortOrder: SortOrder?,
	val tags: Set<MangaTag>,
) {

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as FilterState

		if (sortOrder != other.sortOrder) return false
		if (tags != other.tags) return false

		return true
	}

	override fun hashCode(): Int {
		var result = sortOrder?.hashCode() ?: 0
		result = 31 * result + tags.hashCode()
		return result
	}
}