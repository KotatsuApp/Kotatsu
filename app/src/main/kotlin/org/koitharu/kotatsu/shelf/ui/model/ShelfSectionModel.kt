package org.koitharu.kotatsu.shelf.ui.model

import android.content.res.Resources
import androidx.annotation.StringRes
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.model.MangaItemModel

sealed interface ShelfSectionModel : ListModel {

	val items: List<MangaItemModel>

	@get:StringRes
	val showAllButtonText: Int

	val key: String
	fun getTitle(resources: Resources): CharSequence

	override fun toString(): String

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is ShelfSectionModel && key == other.key
	}

	override fun getChangePayload(previousState: ListModel): Any? {
		return if (previousState is ShelfSectionModel) {
			Unit
		} else {
			null
		}
	}

	class History(
		override val items: List<MangaItemModel>,
		override val showAllButtonText: Int,
	) : ShelfSectionModel {

		override val key = "history"

		override fun getTitle(resources: Resources) = resources.getString(R.string.history)

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as History

			if (showAllButtonText != other.showAllButtonText) return false
			return items == other.items
		}

		override fun hashCode(): Int {
			var result = items.hashCode()
			result = 31 * result + showAllButtonText.hashCode()
			return result
		}

		override fun toString(): String = key
	}

	class Favourites(
		override val items: List<MangaItemModel>,
		val category: FavouriteCategory,
		override val showAllButtonText: Int,
	) : ShelfSectionModel {

		override val key = "fav_${category.id}"

		override fun getTitle(resources: Resources) = category.title

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as Favourites

			if (category != other.category) return false
			if (showAllButtonText != other.showAllButtonText) return false
			return items == other.items
		}

		override fun hashCode(): Int {
			var result = items.hashCode()
			result = 31 * result + category.hashCode()
			result = 31 * result + showAllButtonText.hashCode()
			return result
		}

		override fun toString(): String = key
	}

	class Updated(
		override val items: List<MangaItemModel>,
		override val showAllButtonText: Int,
	) : ShelfSectionModel {

		override val key = "upd"

		override fun getTitle(resources: Resources) = resources.getString(R.string.updated)

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as Updated

			if (items != other.items) return false
			return showAllButtonText == other.showAllButtonText
		}

		override fun hashCode(): Int {
			var result = items.hashCode()
			result = 31 * result + showAllButtonText
			return result
		}

		override fun toString(): String = key
	}

	class Local(
		override val items: List<MangaItemModel>,
		override val showAllButtonText: Int,
	) : ShelfSectionModel {

		override val key = "local"

		override fun getTitle(resources: Resources) = resources.getString(R.string.local_storage)

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as Local

			if (items != other.items) return false
			return showAllButtonText == other.showAllButtonText
		}

		override fun hashCode(): Int {
			var result = items.hashCode()
			result = 31 * result + showAllButtonText
			return result
		}

		override fun toString(): String = key
	}

	class Suggestions(
		override val items: List<MangaItemModel>,
		override val showAllButtonText: Int,
	) : ShelfSectionModel {

		override val key = "suggestions"

		override fun getTitle(resources: Resources) = resources.getString(R.string.suggestions)

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as Suggestions

			if (items != other.items) return false
			return showAllButtonText == other.showAllButtonText
		}

		override fun hashCode(): Int {
			var result = items.hashCode()
			result = 31 * result + showAllButtonText
			return result
		}

		override fun toString(): String = key
	}
}
