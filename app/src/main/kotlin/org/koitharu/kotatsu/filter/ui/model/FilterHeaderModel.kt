package org.koitharu.kotatsu.filter.ui.model

import org.koitharu.kotatsu.core.ui.widgets.ChipsView
import org.koitharu.kotatsu.parsers.model.SortOrder

class FilterHeaderModel(
	val chips: Collection<ChipsView.ChipModel>,
	val sortOrder: SortOrder?,
	val hasSelectedTags: Boolean,
) {

	val textSummary: String
		get() = chips.mapNotNull { if (it.isChecked) it.title else null }.joinToString()

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as FilterHeaderModel

		if (chips != other.chips) return false
		return sortOrder == other.sortOrder
		// Not need to check hasSelectedTags

	}

	override fun hashCode(): Int {
		var result = chips.hashCode()
		result = 31 * result + (sortOrder?.hashCode() ?: 0)
		return result
	}
}
