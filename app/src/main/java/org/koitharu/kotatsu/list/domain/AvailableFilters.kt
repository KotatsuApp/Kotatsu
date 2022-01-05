package org.koitharu.kotatsu.list.domain

import org.koitharu.kotatsu.core.model.MangaTag
import org.koitharu.kotatsu.core.model.SortOrder

class AvailableFilters(
	val sortOrders: Set<SortOrder>,
	val tags: Set<MangaTag>,
) {

	val size: Int
		get() = sortOrders.size + tags.size

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false
		other as AvailableFilters
		if (sortOrders != other.sortOrders) return false
		if (tags != other.tags) return false
		return true
	}

	override fun hashCode(): Int {
		var result = sortOrders.hashCode()
		result = 31 * result + tags.hashCode()
		return result
	}

	fun isEmpty(): Boolean = sortOrders.isEmpty() && tags.isEmpty()
}