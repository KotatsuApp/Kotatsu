package org.koitharu.kotatsu.explore.ui.model

import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.parsers.model.Manga

class RecommendationsItem(
	val manga: Manga
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is RecommendationsItem
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as RecommendationsItem

		return manga == other.manga
	}

	override fun hashCode(): Int {
		return 31 * manga.hashCode()
	}

}
