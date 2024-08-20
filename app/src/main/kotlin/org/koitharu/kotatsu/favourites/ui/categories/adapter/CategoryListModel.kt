package org.koitharu.kotatsu.favourites.ui.categories.adapter

import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.favourites.domain.model.Cover
import org.koitharu.kotatsu.list.ui.ListModelDiffCallback
import org.koitharu.kotatsu.list.ui.model.ListModel

class CategoryListModel(
	val mangaCount: Int,
	val covers: List<Cover>,
	val category: FavouriteCategory,
	val isTrackerEnabled: Boolean,
	val isActionsEnabled: Boolean,
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is CategoryListModel && other.category.id == category.id
	}

	override fun getChangePayload(previousState: ListModel): Any? = when {
		previousState !is CategoryListModel -> super.getChangePayload(previousState)
		previousState.isActionsEnabled != isActionsEnabled -> ListModelDiffCallback.PAYLOAD_ANYTHING_CHANGED
		else -> super.getChangePayload(previousState)
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as CategoryListModel

		if (mangaCount != other.mangaCount) return false
		if (isTrackerEnabled != other.isTrackerEnabled) return false
		if (isActionsEnabled != other.isActionsEnabled) return false
		if (covers != other.covers) return false
		if (category.id != other.category.id) return false
		if (category.title != other.category.title) return false
		// ignore the category.sortKey field
		if (category.order != other.category.order) return false
		if (category.createdAt != other.category.createdAt) return false
		if (category.isTrackingEnabled != other.category.isTrackingEnabled) return false
		return category.isVisibleInLibrary == other.category.isVisibleInLibrary
	}

	override fun hashCode(): Int {
		var result = mangaCount
		result = 31 * result + isTrackerEnabled.hashCode()
		result = 31 * result + isActionsEnabled.hashCode()
		result = 31 * result + covers.hashCode()
		result = 31 * result + category.id.hashCode()
		result = 31 * result + category.title.hashCode()
		// ignore the category.sortKey field
		result = 31 * result + category.order.hashCode()
		result = 31 * result + category.createdAt.hashCode()
		result = 31 * result + category.isTrackingEnabled.hashCode()
		result = 31 * result + category.isVisibleInLibrary.hashCode()
		return result
	}

	override fun toString(): String {
		return "CategoryListModel(categoryId=${category.id})"
	}
}
