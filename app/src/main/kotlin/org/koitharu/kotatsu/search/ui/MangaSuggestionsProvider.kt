package org.koitharu.kotatsu.search.ui

import android.app.SearchManager
import android.content.ContentResolver
import android.content.Context
import android.content.SearchRecentSuggestionsProvider
import android.net.Uri
import android.provider.SearchRecentSuggestions
import org.koitharu.kotatsu.BuildConfig

class MangaSuggestionsProvider : SearchRecentSuggestionsProvider() {

	init {
		setupSuggestions(AUTHORITY, MODE)
	}

	companion object {

		private const val AUTHORITY = "${BuildConfig.APPLICATION_ID}.MangaSuggestionsProvider"
		private const val MODE = DATABASE_MODE_QUERIES

		fun createSuggestions(context: Context): SearchRecentSuggestions {
			return SearchRecentSuggestions(context, AUTHORITY, MODE)
		}

		val QUERY_URI: Uri = Uri.Builder()
			.scheme(ContentResolver.SCHEME_CONTENT)
			.authority(AUTHORITY)
			.appendPath(SearchManager.SUGGEST_URI_PATH_QUERY)
			.build()

		val URI: Uri = Uri.parse("content://$AUTHORITY/suggestions")
	}
}