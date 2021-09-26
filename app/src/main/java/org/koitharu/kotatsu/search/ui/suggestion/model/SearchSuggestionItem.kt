package org.koitharu.kotatsu.search.ui.suggestion.model

import kotlinx.coroutines.flow.MutableStateFlow
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaSource

sealed class SearchSuggestionItem {

	data class MangaItem(
		val manga: Manga,
	) : SearchSuggestionItem()

	data class RecentQuery(
		val query: String,
	) : SearchSuggestionItem()

	data class Header(
		val source: MangaSource,
		val isChecked: MutableStateFlow<Boolean>,
	) : SearchSuggestionItem()
}
