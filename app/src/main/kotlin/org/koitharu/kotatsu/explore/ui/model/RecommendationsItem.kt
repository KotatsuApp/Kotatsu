package org.koitharu.kotatsu.explore.ui.model

import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.model.MangaListModel

data class RecommendationsItem(
	val manga: List<MangaListModel>
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is RecommendationsItem
	}
}
