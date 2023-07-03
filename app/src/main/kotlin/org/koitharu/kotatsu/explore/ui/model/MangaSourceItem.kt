package org.koitharu.kotatsu.explore.ui.model

import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.parsers.model.MangaSource

class MangaSourceItem(
	val source: MangaSource,
	val isGrid: Boolean,
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is MangaSourceItem && other.source == source
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as MangaSourceItem

		if (source != other.source) return false
		return isGrid == other.isGrid
	}

	override fun hashCode(): Int {
		var result = source.hashCode()
		result = 31 * result + isGrid.hashCode()
		return result
	}
}
