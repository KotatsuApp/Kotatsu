package org.koitharu.kotatsu.explore.ui.model

import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.parsers.model.Manga

data class RecommendationsItem(
	val manga: Manga
) : ListModel {
	val summary: String = manga.tags.joinToString { it.title }

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is RecommendationsItem
	}
}
