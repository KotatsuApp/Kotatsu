package org.koitharu.kotatsu.favourites.ui.categories.adapter

import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.favourites.domain.model.Cover
import org.koitharu.kotatsu.list.ui.model.ListModel

class CategoryListModel(
	val mangaCount: Int,
	val covers: List<Cover>,
	val category: FavouriteCategory,
	val isReorderMode: Boolean,
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is CategoryListModel && other.category.id == category.id
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as CategoryListModel

		if (mangaCount != other.mangaCount) return false
		if (isReorderMode != other.isReorderMode) return false
		if (covers != other.covers) return false
		if (category.id != other.category.id) return false
		if (category.title != other.category.title) return false
		return category.order == other.category.order
	}

	override fun hashCode(): Int {
		var result = mangaCount
		result = 31 * result + isReorderMode.hashCode()
		result = 31 * result + covers.hashCode()
		result = 31 * result + category.id.hashCode()
		result = 31 * result + category.title.hashCode()
		result = 31 * result + category.order.hashCode()
		return result
	}
}
