package org.koitharu.kotatsu.search.ui.suggestion

import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.model.MangaTag

interface SearchSuggestionListener {

	fun onMangaClick(manga: Manga)

	fun onQueryClick(query: String, submit: Boolean)

	fun onQueryChanged(query: String)

	fun onSourceToggle(source: MangaSource, isEnabled: Boolean)

	fun onSourceClick(source: MangaSource)

	fun onTagClick(tag: MangaTag)
}
