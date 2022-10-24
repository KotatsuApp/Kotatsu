package org.koitharu.kotatsu.shelf.ui.config.categories

import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.shelf.domain.ShelfSection

sealed interface ShelfConfigModel : ListModel {

	val isChecked: Boolean

	class Section(
		val section: ShelfSection,
		override val isChecked: Boolean,
	) : ShelfConfigModel {

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as Section

			if (section != other.section) return false
			if (isChecked != other.isChecked) return false

			return true
		}

		override fun hashCode(): Int {
			var result = section.hashCode()
			result = 31 * result + isChecked.hashCode()
			return result
		}
	}

	class FavouriteCategory(
		val id: Long,
		val title: String,
		override val isChecked: Boolean,
	) : ShelfConfigModel {

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as FavouriteCategory

			if (id != other.id) return false
			if (title != other.title) return false
			if (isChecked != other.isChecked) return false

			return true
		}

		override fun hashCode(): Int {
			var result = id.hashCode()
			result = 31 * result + title.hashCode()
			result = 31 * result + isChecked.hashCode()
			return result
		}
	}
}
