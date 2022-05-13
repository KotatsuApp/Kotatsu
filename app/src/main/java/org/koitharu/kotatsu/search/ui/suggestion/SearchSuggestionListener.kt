package org.koitharu.kotatsu.search.ui.suggestion

import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaTag

interface SearchSuggestionListener {

	fun onMangaClick(manga: Manga)

	fun onQueryClick(query: String, submit: Boolean)

	fun onQueryChanged(query: String)

	fun onClearSearchHistory()

	fun onTagClick(tag: MangaTag)

	fun onVoiceSearchClick()
}