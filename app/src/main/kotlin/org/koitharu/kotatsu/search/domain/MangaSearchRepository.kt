package org.koitharu.kotatsu.search.domain

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Context
import android.provider.SearchRecentSuggestions
import dagger.Reusable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.db.entity.toManga
import org.koitharu.kotatsu.core.db.entity.toMangaTag
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.parsers.util.levenshteinDistance
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import org.koitharu.kotatsu.search.ui.MangaSuggestionsProvider
import javax.inject.Inject

@Reusable
class MangaSearchRepository @Inject constructor(
	private val settings: AppSettings,
	private val db: MangaDatabase,
	@ApplicationContext private val context: Context,
	private val recentSuggestions: SearchRecentSuggestions,
	private val mangaRepositoryFactory: MangaRepository.Factory,
) {

	fun globalSearch(query: String, concurrency: Int = DEFAULT_CONCURRENCY): Flow<Manga> =
		settings.getMangaSources(includeHidden = false).asFlow()
			.flatMapMerge(concurrency) { source ->
				runCatchingCancellable {
					mangaRepositoryFactory.create(source).getList(
						offset = 0,
						query = query,
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
			"date DESC",
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

	suspend fun getTagsSuggestion(query: String, limit: Int, source: MangaSource?): List<MangaTag> {
		return when {
			query.isNotEmpty() && source != null -> db.tagsDao.findTags(source.name, "%$query%", limit)
			query.isNotEmpty() -> db.tagsDao.findTags("%$query%", limit)
			source != null -> db.tagsDao.findPopularTags(source.name, limit)
			else -> db.tagsDao.findPopularTags(limit)
		}.map {
			it.toMangaTag()
		}
	}

	fun getSourcesSuggestion(query: String, limit: Int): List<MangaSource> {
		if (query.length < 3) {
			return emptyList()
		}
		val sources = settings.remoteMangaSources
			.filter { x -> x.title.contains(query, ignoreCase = true) }
		return if (limit == 0) {
			sources
		} else {
			sources.take(limit)
		}
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
			null,
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
