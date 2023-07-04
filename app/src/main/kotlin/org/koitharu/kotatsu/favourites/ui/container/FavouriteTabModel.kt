package org.koitharu.kotatsu.favourites.ui.container

import org.koitharu.kotatsu.list.ui.model.ListModel

class FavouriteTabModel(
	val id: Long,
	val title: String,
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is FavouriteTabModel && other.id == id
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as FavouriteTabModel

		if (id != other.id) return false
		return title == other.title
	}

	override fun hashCode(): Int {
		var result = id.hashCode()
		result = 31 * result + title.hashCode()
		return result
	}
}
