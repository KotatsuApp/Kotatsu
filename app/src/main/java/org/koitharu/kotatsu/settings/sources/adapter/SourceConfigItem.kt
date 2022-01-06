package org.koitharu.kotatsu.settings.sources.adapter

import org.koitharu.kotatsu.core.model.MangaSource

sealed interface SourceConfigItem {

	data class LocaleHeader(
		val localeId: String?,
		val title: String?,
		val isExpanded: Boolean,
	) : SourceConfigItem

	data class SourceItem(
		val source: MangaSource,
		val isEnabled: Boolean,
	) : SourceConfigItem
}