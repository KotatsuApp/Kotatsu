package org.koitharu.kotatsu.explore.ui.model

import org.koitharu.kotatsu.core.util.ext.longHashCode
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.parsers.model.MangaSource

data class MangaSourceItem(
	val source: MangaSource,
	val isGrid: Boolean,
) : ListModel {

	val id: Long = source.name.longHashCode()

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is MangaSourceItem && other.source == source
	}
}
