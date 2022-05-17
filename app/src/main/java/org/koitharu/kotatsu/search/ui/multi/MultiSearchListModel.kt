package org.koitharu.kotatsu.search.ui.multi

import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.model.MangaItemModel
import org.koitharu.kotatsu.parsers.model.MangaSource

class MultiSearchListModel(
	val source: MangaSource,
	val list: List<MangaItemModel>,
) : ListModel {

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as MultiSearchListModel

		if (source != other.source) return false
		if (list != other.list) return false

		return true
	}

	override fun hashCode(): Int {
		var result = source.hashCode()
		result = 31 * result + list.hashCode()
		return result
	}
}