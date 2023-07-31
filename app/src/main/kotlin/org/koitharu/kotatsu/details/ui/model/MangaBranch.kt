package org.koitharu.kotatsu.details.ui.model

import org.koitharu.kotatsu.list.ui.ListModelDiffCallback
import org.koitharu.kotatsu.list.ui.model.ListModel

class MangaBranch(
	val name: String?,
	val count: Int,
	val isSelected: Boolean,
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is MangaBranch && other.name == name
	}

	override fun getChangePayload(previousState: ListModel): Any? {
		return if (previousState is MangaBranch && previousState.isSelected != isSelected) {
			ListModelDiffCallback.PAYLOAD_CHECKED_CHANGED
		} else {
			super.getChangePayload(previousState)
		}
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as MangaBranch

		if (name != other.name) return false
		if (count != other.count) return false
		return isSelected == other.isSelected
	}

	override fun hashCode(): Int {
		var result = name.hashCode()
		result = 31 * result + count
		result = 31 * result + isSelected.hashCode()
		return result
	}

	override fun toString(): String {
		return "$name: $count"
	}
}
