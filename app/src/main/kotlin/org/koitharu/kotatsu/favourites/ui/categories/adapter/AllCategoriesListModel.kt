package org.koitharu.kotatsu.favourites.ui.categories.adapter

import org.koitharu.kotatsu.favourites.domain.model.Cover
import org.koitharu.kotatsu.list.ui.model.ListModel

data class AllCategoriesListModel(
	val mangaCount: Int,
	val covers: List<Cover>,
	val isVisible: Boolean,
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is AllCategoriesListModel
	}
}
