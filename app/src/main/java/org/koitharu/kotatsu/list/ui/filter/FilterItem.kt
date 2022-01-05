package org.koitharu.kotatsu.list.ui.filter

import androidx.annotation.StringRes
import org.koitharu.kotatsu.core.model.MangaTag
import org.koitharu.kotatsu.core.model.SortOrder

sealed interface FilterItem {

	class Header(
		@StringRes val titleResId: Int,
	) : FilterItem

	class Sort(
		val order: SortOrder,
		val isSelected: Boolean,
	) : FilterItem

	class Tag(
		val tag: MangaTag,
		val isChecked: Boolean,
	) : FilterItem
}
