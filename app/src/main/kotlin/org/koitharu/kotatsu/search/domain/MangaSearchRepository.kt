package org.koitharu.kotatsu.search.domain

import android.app.SearchManager
import android.content.Context
import android.provider.SearchRecentSuggestions
import dagger.Reusable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.db.entity.MangaWithTags
import org.koitharu.kotatsu.core.db.entity.toEntity
import org.koitharu.kotatsu.core.db.entity.toManga
import org.koitharu.kotatsu.core.db.entity.toMangaTag
import org.koitharu.kotatsu.core.db.entity.toMangaTagsList
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.explore.data.MangaSourcesRepository
import org.koitharu.kotatsu.parsers.model.ContentType
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.parsers.util.levenshteinDistance
import org.koitharu.kotatsu.parsers.util.mapToSet
import org.koitharu.kotatsu.search.ui.MangaSuggestionsProvider
import javax.inject.Inject

@Reusable
class MangaSearchRepository @Inject constructor(
	private val db: MangaDatabase,
	private val sourcesRepository: MangaSourcesRepository,
	@ApplicationContext private val context: Context,
	private val recentSuggestions: SearchRecentSuggestions,
	private val settings: AppSettings,
) {

	suspend fun getMangaSuggestion(query: String, limit: Int, source: MangaSource?): List<Manga> {
		return when {
			query.isEmpty() -> db.getSuggestionDao().getRandom(limit).map { MangaWithTags(it.manga, it.tags) }
			source != null -> db.getMangaDao().searchByTitle("%$query%", source.name, limit)
			else -> db.getMangaDao().searchByTitle("%$query%", limit)
		}.let {
			if (settings.isNsfwContentDisabled) it.filterNot { x -> x.manga.isNsfw } else it
		}.map {
			it.toManga()
		}.sortedBy { x ->
			x.title.levenshteinDistance(query)
		}
	}

	suspend fun getQuerySuggestion(
		query: String,
		limit: Int,
	): List<String> = withContext(Dispatchers.IO) {
		context.contentResolver.query(
			MangaSuggestionsProvider.QUERY_URI,
			arrayOf(SearchManager.SUGGEST_COLUMN_QUERY),
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

	suspend fun getQueryHintSuggestion(
		query: String,
		limit: Int,
	): List<String> {
		if (query.isEmpty()) {
			return emptyList()
		}
		val titles = db.getSuggestionDao().getTitles("$query%")
		if (titles.isEmpty()) {
			return emptyList()
		}
		return titles.shuffled().take(limit)
	}

	suspend fun getAuthorsSuggestion(
		query: String,
		limit: Int,
	): List<String> {
		if (query.isEmpty()) {
			return emptyList()
		}
		return db.getMangaDao().findAuthors("$query%", limit)
	}

	suspend fun getTagsSuggestion(query: String, limit: Int, source: MangaSource?): List<MangaTag> {
		return when {
			query.isNotEmpty() && source != null -> db.getTagsDao()
				.findTags(source.name, "%$query%", limit)

			query.isNotEmpty() -> db.getTagsDao().findTags("%$query%", limit)
			source != null -> db.getTagsDao().findPopularTags(source.name, limit)
			else -> db.getTagsDao().findPopularTags(limit)
		}.toMangaTagsList()
	}

	suspend fun getTagsSuggestion(tags: Set<MangaTag>): List<MangaTag> {
		val ids = tags.mapToSet { it.toEntity().id }
		return if (ids.size == 1) {
			db.getTagsDao().findRelatedTags(ids.first())
		} else {
			db.getTagsDao().findRelatedTags(ids)
		}.mapNotNull { x ->
			if (x.id in ids) null else x.toMangaTag()
		}
	}

	suspend fun getRareTags(source: MangaSource, limit: Int): List<MangaTag> {
		return db.getTagsDao().findRareTags(source.name, limit).toMangaTagsList()
	}

	suspend fun getSourcesSuggestion(limit: Int): List<MangaSource> = sourcesRepository.getTopSources(limit)

	fun getSourcesSuggestion(query: String, limit: Int): List<MangaSource> {
		if (query.length < 3) {
			return emptyList()
		}
		val skipNsfw = settings.isNsfwContentDisabled
		val sources = sourcesRepository.allMangaSources
			.filter { x ->
				(x.contentType != ContentType.HENTAI || !skipNsfw) && x.title.contains(query, ignoreCase = true)
			}
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
			arrayOf(SearchManager.SUGGEST_COLUMN_QUERY),
			null,
			arrayOfNulls(1),
			null,
		)?.use { cursor -> cursor.count } ?: 0
	}
}
