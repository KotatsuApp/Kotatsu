package org.koitharu.kotatsu.explore.ui.model

import org.koitharu.kotatsu.core.model.MangaSourceInfo
import org.koitharu.kotatsu.core.util.ext.longHashCode
import org.koitharu.kotatsu.list.ui.model.ListModel

data class MangaSourceItem(
	val source: MangaSourceInfo,
	val isGrid: Boolean,
) : ListModel {

	val id: Long = source.name.longHashCode()

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is MangaSourceItem && other.source == source
	}
}
