package org.koitharu.kotatsu.favourites.ui.categories.adapter

import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.list.ui.model.ListModel

sealed interface CategoryListModel : ListModel {

	val id: Long

	class All(
		val isVisible: Boolean,
	) : CategoryListModel {

		override val id: Long = 0L

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as All

			if (isVisible != other.isVisible) return false

			return true
		}

		override fun hashCode(): Int {
			return isVisible.hashCode()
		}
	}

	class CategoryItem(
		val category: FavouriteCategory,
	) : CategoryListModel {

		override val id: Long
			get() = category.id

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as CategoryItem

			if (category.id != other.category.id) return false
			if (category.title != other.category.title) return false
			if (category.order != other.category.order) return false
			if (category.isTrackingEnabled != other.category.isTrackingEnabled) return false

			return true
		}

		override fun hashCode(): Int {
			var result = category.id.hashCode()
			result = 31 * result + category.title.hashCode()
			result = 31 * result + category.order.hashCode()
			result = 31 * result + category.isTrackingEnabled.hashCode()
			return result
		}
	}
}