package org.koitharu.kotatsu.shelf.ui.model

import android.content.res.Resources
import androidx.annotation.StringRes
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.core.ui.DateTimeAgo
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.model.MangaItemModel

sealed class ShelfSectionModel(
	val items: List<MangaItemModel>,
	@StringRes val showAllButtonText: Int,
) : ListModel {

	abstract val key: Any
	abstract fun getTitle(resources: Resources): CharSequence

	class History(
		items: List<MangaItemModel>,
		val timeAgo: DateTimeAgo?,
		showAllButtonText: Int,
	) : ShelfSectionModel(items, showAllButtonText) {

		override val key: Any
			get() = timeAgo?.javaClass ?: this::class.java

		override fun getTitle(resources: Resources): CharSequence {
			return timeAgo?.format(resources) ?: resources.getString(R.string.history)
		}

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as History

			if (timeAgo != other.timeAgo) return false
			if (showAllButtonText != other.showAllButtonText) return false
			if (items != other.items) return false

			return true
		}

		override fun hashCode(): Int {
			var result = items.hashCode()
			result = 31 * result + (timeAgo?.hashCode() ?: 0)
			result = 31 * result + showAllButtonText.hashCode()
			return result
		}

		override fun toString(): String {
			return "hist_$timeAgo"
		}
	}

	class Favourites(
		items: List<MangaItemModel>,
		val category: FavouriteCategory,
		showAllButtonText: Int,
	) : ShelfSectionModel(items, showAllButtonText) {

		override val key: Any
			get() = category.id

		override fun getTitle(resources: Resources): CharSequence {
			return category.title
		}

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as Favourites

			if (category != other.category) return false
			if (showAllButtonText != other.showAllButtonText) return false
			if (items != other.items) return false

			return true
		}

		override fun hashCode(): Int {
			var result = items.hashCode()
			result = 31 * result + category.hashCode()
			result = 31 * result + showAllButtonText.hashCode()
			return result
		}

		override fun toString(): String {
			return "fav_${category.id}"
		}
	}
}
