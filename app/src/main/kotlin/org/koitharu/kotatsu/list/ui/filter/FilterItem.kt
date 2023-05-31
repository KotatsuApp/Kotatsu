package org.koitharu.kotatsu.list.ui.filter

import androidx.annotation.StringRes
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.parsers.model.SortOrder

sealed interface FilterItem : ListModel {

	class Sort(
		val order: SortOrder,
		val isSelected: Boolean,
	) : FilterItem {

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as Sort

			if (order != other.order) return false
			return isSelected == other.isSelected
		}

		override fun hashCode(): Int {
			var result = order.hashCode()
			result = 31 * result + isSelected.hashCode()
			return result
		}
	}

	class Tag(
		val tag: MangaTag,
		val isChecked: Boolean,
	) : FilterItem {

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as Tag

			if (tag != other.tag) return false
			return isChecked == other.isChecked
		}

		override fun hashCode(): Int {
			var result = tag.hashCode()
			result = 31 * result + isChecked.hashCode()
			return result
		}
	}

	class Error(
		@StringRes val textResId: Int,
	) : FilterItem {

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as Error

			return textResId == other.textResId
		}

		override fun hashCode(): Int {
			return textResId
		}
	}
}
