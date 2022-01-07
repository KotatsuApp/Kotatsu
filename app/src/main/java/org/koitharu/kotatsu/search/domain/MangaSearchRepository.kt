package org.koitharu.kotatsu.search.domain

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Context
import android.provider.SearchRecentSuggestions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.base.domain.MangaProviderFactory
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.model.SortOrder
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.search.ui.MangaSuggestionsProvider
import org.koitharu.kotatsu.utils.ext.levenshteinDistance

class MangaSearchRepository(
	private val settings: AppSettings,
	private val db: MangaDatabase,
	private val context: Context,
	private val recentSuggestions: SearchRecentSuggestions,
) {

	fun globalSearch(query: String, concurrency: Int = DEFAULT_CONCURRENCY): Flow<Manga> =
		MangaProviderFactory.getSources(settings, includeHidden = false).asFlow()
			.flatMapMerge(concurrency) { source ->
				runCatching {
					source.repository.getList2(
						offset = 0,
						query = query,
						sortOrder = SortOrder.POPULARITY
					)
				}.getOrElse {
					emptyList()
				}.asFlow()
			}.filter {
				match(it, query)
			}

	suspend fun getMangaSuggestion(query: String, limit: Int, source: MangaSource?): List<Manga> {
		if (query.isEmpty()) {
			return emptyList()
		}
		return if (source != null) {
			db.mangaDao.searchByTitle("%$query%", source.name, limit)
		} else {
			db.mangaDao.searchByTitle("%$query%", limit)
		}.map { it.toManga() }
			.sortedBy { x -> x.title.levenshteinDistance(query) }
	}

	suspend fun getQuerySuggestion(
		query: String,
		limit: Int,
	): List<String> = withContext(Dispatchers.IO) {
		context.contentResolver.query(
			MangaSuggestionsProvider.QUERY_URI,
			SUGGESTION_PROJECTION,
			"${SearchManager.SUGGEST_COLUMN_QUERY} LIKE ?",
			arrayOf("%$query%"),
			"date DESC"
		)?.use { cursor ->
			val count = minOf(cursor.count, limit)
			if (count == 0) {
				return@withContext emptyList()
			}
			val result = ArrayList<String>(count)
			if (cursor.moveToFirst()) {
				val index = cursor.getColumnIndexOrThrow(SearchManager.SUGGEST_COLUMN_QUERY)
				do {
					result += cursor.getString(index)
				} while (currentCoroutineContext().isActive && cursor.moveToNext())
			}
			result
		}.orEmpty()
	}

	fun saveSearchQuery(query: String) {
		recentSuggestions.saveRecentQuery(query, null)
	}

	suspend fun clearSearchHistory(): Unit = withContext(Dispatchers.IO) {
		recentSuggestions.clearHistory()
	}

	suspend fun deleteSearchQuery(query: String) = withContext(Dispatchers.IO) {
		context.contentResolver.delete(
			MangaSuggestionsProvider.URI,
			"display1 = ?",
			arrayOf(query),
		)
	}

	suspend fun getSearchHistoryCount(): Int = withContext(Dispatchers.IO) {
		context.contentResolver.query(
			MangaSuggestionsProvider.QUERY_URI,
			SUGGESTION_PROJECTION,
			null,
			arrayOfNulls(1),
			null
		)?.use { cursor -> cursor.count } ?: 0
	}

	private companion object {

		private val REGEX_SPACE = Regex("\\s+")
		val SUGGESTION_PROJECTION = arrayOf(SearchManager.SUGGEST_COLUMN_QUERY)

		@SuppressLint("DefaultLocale")
		fun match(manga: Manga, query: String): Boolean {
			val words = HashSet<String>()
			words += manga.title.lowercase().split(REGEX_SPACE)
			words += manga.altTitle?.lowercase()?.split(REGEX_SPACE).orEmpty()
			val words2 = query.lowercase().split(REGEX_SPACE).toSet()
			for (w in words) {
				for (w2 in words2) {
					val diff = w.levenshteinDistance(w2) / ((w.length + w2.length) / 2f)
					if (diff < 0.5) {
						return true
					}
				}
			}
			return false
		}
	}
}