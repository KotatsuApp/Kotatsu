package org.koitharu.kotatsu.explore.ui.model

import org.koitharu.kotatsu.list.ui.model.ListModel

class ExploreButtons(
	val isRandomLoading: Boolean,
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is ExploreButtons
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as ExploreButtons

		return isRandomLoading == other.isRandomLoading
	}

	override fun hashCode(): Int {
		return isRandomLoading.hashCode()
	}
}
