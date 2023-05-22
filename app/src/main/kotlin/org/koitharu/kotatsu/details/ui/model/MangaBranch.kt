package org.koitharu.kotatsu.details.ui.model

import org.koitharu.kotatsu.list.ui.model.ListModel

class MangaBranch(
	val name: String?,
	val count: Int,
	val isSelected: Boolean,
) : ListModel {

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
