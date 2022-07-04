package org.koitharu.kotatsu.library.ui.model

import android.content.res.Resources
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.core.ui.DateTimeAgo
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.model.MangaItemModel

sealed class LibraryGroupModel(
	val items: List<MangaItemModel>
) : ListModel {

	abstract val key: Any
	abstract fun getTitle(resources: Resources): CharSequence

	class History(
		items: List<MangaItemModel>,
		val timeAgo: DateTimeAgo?,
	) : LibraryGroupModel(items) {

		override val key: Any
			get() = timeAgo?.javaClass ?: this::class.java

		override fun getTitle(resources: Resources): CharSequence {
			return timeAgo?.format(resources) ?: resources.getString(R.string.history)
		}

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as History

			if (items != other.items) return false
			if (timeAgo != other.timeAgo) return false

			return true
		}

		override fun hashCode(): Int {
			var result = items.hashCode()
			result = 31 * result + (timeAgo?.hashCode() ?: 0)
			return result
		}
	}

	class Favourites(
		items: List<MangaItemModel>,
		val category: FavouriteCategory,
	) : LibraryGroupModel(items) {

		override val key: Any
			get() = category.id

		override fun getTitle(resources: Resources): CharSequence {
			return category.title
		}

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as Favourites

			if (items != other.items) return false
			if (category != other.category) return false

			return true
		}

		override fun hashCode(): Int {
			var result = items.hashCode()
			result = 31 * result + category.hashCode()
			return result
		}
	}
}