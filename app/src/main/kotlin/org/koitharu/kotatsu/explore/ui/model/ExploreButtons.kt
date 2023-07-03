package org.koitharu.kotatsu.explore.ui.model

import org.koitharu.kotatsu.list.ui.model.ListModel

class ExploreButtons : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is ExploreButtons
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		return javaClass == other?.javaClass
	}

	override fun hashCode(): Int {
		return javaClass.hashCode()
	}
}
