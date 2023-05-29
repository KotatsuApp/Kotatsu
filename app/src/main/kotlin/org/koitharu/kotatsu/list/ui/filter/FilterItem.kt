package org.koitharu.kotatsu.list.ui.filter

import androidx.annotation.StringRes
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.parsers.model.SortOrder

sealed interface FilterItem {

	class Header(
		@StringRes val titleResId: Int,
		val counter: Int,
	) : FilterItem

	class Sort(
		val order: SortOrder,
		val isSelected: Boolean,
	) : FilterItem

	class Tag(
		val tag: MangaTag,
		val isChecked: Boolean,
	) : FilterItem

	object Loading : FilterItem

	class Error(
		@StringRes val textResId: Int,
	) : FilterItem
}
