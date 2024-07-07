package org.koitharu.kotatsu.core.prefs

import androidx.annotation.StringRes
import org.koitharu.kotatsu.R

enum class SearchSuggestionType(
	@StringRes val titleResId: Int,
) {

	GENRES(R.string.genres),
	QUERIES_RECENT(R.string.recent_queries),
	QUERIES_SUGGEST(R.string.suggested_queries),
	MANGA(R.string.content_type_manga),
	SOURCES(R.string.remote_sources),
	RECENT_SOURCES(R.string.recent_sources),
	AUTHORS(R.string.authors),
}
