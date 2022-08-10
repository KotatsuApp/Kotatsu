package org.koitharu.kotatsu.list.ui.model

import org.koitharu.kotatsu.base.ui.widgets.ChipsView
import org.koitharu.kotatsu.parsers.model.SortOrder

class ListHeader2(
	val chips: Collection<ChipsView.ChipModel>,
	val sortOrder: SortOrder?,
	val hasSelectedTags: Boolean,
) : ListModel {

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as ListHeader2

		if (chips != other.chips) return false
		if (sortOrder != other.sortOrder) return false
		// Not need to check hasSelectedTags

		return true
	}

	override fun hashCode(): Int {
		var result = chips.hashCode()
		result = 31 * result + (sortOrder?.hashCode() ?: 0)
		return result
	}
}