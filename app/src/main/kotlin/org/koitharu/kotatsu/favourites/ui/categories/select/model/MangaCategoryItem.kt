package org.koitharu.kotatsu.favourites.ui.categories.select.model

import org.koitharu.kotatsu.list.ui.ListModelDiffCallback
import org.koitharu.kotatsu.list.ui.model.ListModel

class MangaCategoryItem(
	val id: Long,
	val name: String,
	val isChecked: Boolean,
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is MangaCategoryItem && other.id == id
	}

	override fun getChangePayload(previousState: ListModel): Any? {
		return if (previousState is MangaCategoryItem && previousState.isChecked != isChecked) {
			ListModelDiffCallback.PAYLOAD_CHECKED_CHANGED
		} else {
			super.getChangePayload(previousState)
		}
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as MangaCategoryItem

		if (id != other.id) return false
		if (name != other.name) return false
		return isChecked == other.isChecked
	}

	override fun hashCode(): Int {
		var result = id.hashCode()
		result = 31 * result + name.hashCode()
		result = 31 * result + isChecked.hashCode()
		return result
	}
}
