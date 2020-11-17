package org.koitharu.kotatsu.list.ui

import org.koitharu.kotatsu.core.model.MangaFilter
import org.koitharu.kotatsu.core.model.MangaTag
import org.koitharu.kotatsu.core.model.SortOrder

data class MangaFilterConfig(
	val sortOrders: List<SortOrder>,
	val tags: List<MangaTag>,
	val currentFilter: MangaFilter?
)