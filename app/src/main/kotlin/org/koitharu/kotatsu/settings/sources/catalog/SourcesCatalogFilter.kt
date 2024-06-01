package org.koitharu.kotatsu.settings.sources.catalog

import org.koitharu.kotatsu.parsers.model.ContentType

data class SourcesCatalogFilter(
	val types: Set<ContentType>,
	val locale: String?,
	val isNewOnly: Boolean,
)
