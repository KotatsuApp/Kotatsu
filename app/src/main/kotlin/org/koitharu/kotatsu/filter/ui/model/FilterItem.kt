package org.koitharu.kotatsu.filter.ui.model

import androidx.annotation.StringRes
import org.koitharu.kotatsu.list.ui.ListModelDiffCallback
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.parsers.model.SortOrder

sealed interface FilterItem : ListModel {

	data class Sort(
		val order: SortOrder,
		val isSelected: Boolean,
	) : FilterItem {

		override fun areItemsTheSame(other: ListModel): Boolean {
			return other is Sort && other.order == order
		}

		override fun getChangePayload(previousState: ListModel): Any? {
			return if (previousState is Sort && previousState.isSelected != isSelected) {
				ListModelDiffCallback.PAYLOAD_CHECKED_CHANGED
			} else {
				super.getChangePayload(previousState)
			}
		}
	}

	data class Tag(
		val tag: MangaTag,
		val isChecked: Boolean,
	) : FilterItem {

		override fun areItemsTheSame(other: ListModel): Boolean {
			return other is Tag && other.tag == tag
		}

		override fun getChangePayload(previousState: ListModel): Any? {
			return if (previousState is Tag && previousState.isChecked != isChecked) {
				ListModelDiffCallback.PAYLOAD_CHECKED_CHANGED
			} else {
				super.getChangePayload(previousState)
			}
		}
	}

	data class Error(
		@StringRes val textResId: Int,
	) : FilterItem {

		override fun areItemsTheSame(other: ListModel): Boolean {
			return other is Error && textResId == other.textResId
		}
	}
}
