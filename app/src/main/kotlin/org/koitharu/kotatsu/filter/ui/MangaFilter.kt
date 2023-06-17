package org.koitharu.kotatsu.filter.ui

import kotlinx.coroutines.flow.StateFlow
import org.koitharu.kotatsu.filter.ui.model.FilterHeaderModel
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.parsers.model.MangaTag

interface MangaFilter : OnFilterChangedListener {

	val filterItems: StateFlow<List<ListModel>>

	val header: StateFlow<FilterHeaderModel>

	fun applyFilter(tags: Set<MangaTag>)
}
