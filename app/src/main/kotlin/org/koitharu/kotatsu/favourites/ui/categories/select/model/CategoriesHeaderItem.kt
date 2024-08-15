package org.koitharu.kotatsu.favourites.ui.categories.select.model

import org.koitharu.kotatsu.favourites.domain.model.Cover
import org.koitharu.kotatsu.list.ui.model.ListModel

data class CategoriesHeaderItem(
	val titles: List<String>,
	val covers: List<Cover>,
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is CategoriesHeaderItem
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		return javaClass == other?.javaClass
	}

	override fun hashCode(): Int {
		return javaClass.hashCode()
	}
}
