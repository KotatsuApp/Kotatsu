package org.koitharu.kotatsu.filter.ui.model

import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.parsers.model.SortOrder

data class FilterState(
	val sortOrder: SortOrder?,
	val tags: Set<MangaTag>,
)
