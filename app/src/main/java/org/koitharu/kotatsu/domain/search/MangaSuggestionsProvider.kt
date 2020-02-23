package org.koitharu.kotatsu.domain.search

import android.content.Context
import android.content.SearchRecentSuggestionsProvider
import android.provider.SearchRecentSuggestions
import org.koitharu.kotatsu.BuildConfig

class MangaSuggestionsProvider : SearchRecentSuggestionsProvider() {

	init {
		setupSuggestions(AUTHORITY, MODE)
	}

	companion object {

		fun saveQuery(context: Context, query: String) {
			SearchRecentSuggestions(context, AUTHORITY, MODE)
				.saveRecentQuery(query, null)
		}

		fun clearHistory(context: Context) {
			SearchRecentSuggestions(context, AUTHORITY, MODE)
				.clearHistory()
		}

		private const val AUTHORITY = "${BuildConfig.APPLICATION_ID}.MangaSuggestionsProvider"
		private const val MODE = DATABASE_MODE_QUERIES
	}
}