package org.koitharu.kotatsu.search.ui.suggestion

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.SearchSuggestionType
import org.koitharu.kotatsu.core.prefs.observeAsFlow
import org.koitharu.kotatsu.core.prefs.observeAsStateFlow
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.ui.widgets.ChipsView
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import org.koitharu.kotatsu.explore.data.MangaSourcesRepository
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.parsers.util.mapToSet
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import org.koitharu.kotatsu.search.domain.MangaSearchRepository
import org.koitharu.kotatsu.search.ui.suggestion.model.SearchSuggestionItem
import javax.inject.Inject

private const val DEBOUNCE_TIMEOUT = 300L
private const val MAX_MANGA_ITEMS = 12
private const val MAX_QUERY_ITEMS = 16
private const val MAX_HINTS_ITEMS = 3
private const val MAX_AUTHORS_ITEMS = 2
private const val MAX_TAGS_ITEMS = 8
private const val MAX_SOURCES_ITEMS = 6
private const val MAX_SOURCES_TIPS_ITEMS = 2

@HiltViewModel
class SearchSuggestionViewModel @Inject constructor(
	private val repository: MangaSearchRepository,
	private val settings: AppSettings,
	private val sourcesRepository: MangaSourcesRepository,
) : BaseViewModel() {

	private val query = MutableStateFlow("")
	private val invalidationTrigger = MutableStateFlow(0)

	val isIncognitoModeEnabled = settings.observeAsStateFlow(
		scope = viewModelScope + Dispatchers.Default,
		key = AppSettings.KEY_INCOGNITO_MODE,
		valueProducer = { isIncognitoModeEnabled },
	)

	val suggestion: Flow<List<SearchSuggestionItem>> = combine(
		query.debounce(DEBOUNCE_TIMEOUT),
		sourcesRepository.observeEnabledSources().map { it.mapToSet { x -> x.name } },
		settings.observeAsFlow(AppSettings.KEY_SEARCH_SUGGESTION_TYPES) { searchSuggestionTypes },
		invalidationTrigger,
	)
	{ a, b, c, _ ->
		Triple(a, b, c)
	}.mapLatest { (searchQuery, enabledSources, types) ->
		buildSearchSuggestion(searchQuery, enabledSources, types)
	}.distinctUntilChanged()
		.withErrorHandling()
		.flowOn(Dispatchers.Default)

	fun onQueryChanged(newQuery: String) {
		query.value = newQuery
	}

	fun saveQuery(query: String) {
		if (!settings.isIncognitoModeEnabled) {
			repository.saveSearchQuery(query)
			invalidationTrigger.value++
		}
	}

	fun clearSearchHistory() {
		launchJob(Dispatchers.Default) {
			repository.clearSearchHistory()
			invalidationTrigger.value++
		}
	}

	fun onSourceToggle(source: MangaSource, isEnabled: Boolean) {
		launchJob(Dispatchers.Default) {
			sourcesRepository.setSourcesEnabled(setOf(source), isEnabled)
		}
	}

	fun deleteQuery(query: String) {
		launchJob(Dispatchers.Default) {
			repository.deleteSearchQuery(query)
			invalidationTrigger.value++
		}
	}

	private suspend fun buildSearchSuggestion(
		searchQuery: String,
		enabledSources: Set<String>,
		types: Set<SearchSuggestionType>,
	): List<SearchSuggestionItem> = coroutineScope {
		listOfNotNull(
			if (SearchSuggestionType.GENRES in types) {
				async { getTags(searchQuery) }
			} else {
				null
			},
			if (SearchSuggestionType.MANGA in types) {
				async { getManga(searchQuery) }
			} else {
				null
			},
			if (SearchSuggestionType.QUERIES_RECENT in types) {
				async { getRecentQueries(searchQuery) }
			} else {
				null
			},
			if (SearchSuggestionType.QUERIES_SUGGEST in types) {
				async { getQueryHints(searchQuery) }
			} else {
				null
			},
			if (SearchSuggestionType.SOURCES in types) {
				async { getSources(searchQuery, enabledSources) }
			} else {
				null
			},
			if (SearchSuggestionType.RECENT_SOURCES in types) {
				async { getRecentSources(searchQuery) }
			} else {
				null
			},
			if (SearchSuggestionType.AUTHORS in types) {
				async {
					getAuthors(searchQuery)
				}
			} else {
				null
			},
		).flatMap { it.await() }
	}

	private suspend fun getAuthors(searchQuery: String): List<SearchSuggestionItem> = runCatchingCancellable {
		repository.getAuthorsSuggestion(searchQuery, MAX_AUTHORS_ITEMS)
			.map { SearchSuggestionItem.Author(it) }
	}.getOrElse { e ->
		e.printStackTraceDebug()
		listOf(SearchSuggestionItem.Text(0, e))
	}

	private suspend fun getQueryHints(searchQuery: String): List<SearchSuggestionItem> = runCatchingCancellable {
		repository.getQueryHintSuggestion(searchQuery, MAX_HINTS_ITEMS)
			.map { SearchSuggestionItem.Hint(it) }
	}.getOrElse { e ->
		e.printStackTraceDebug()
		listOf(SearchSuggestionItem.Text(0, e))
	}

	private suspend fun getRecentQueries(searchQuery: String): List<SearchSuggestionItem> = runCatchingCancellable {
		repository.getQuerySuggestion(searchQuery, MAX_QUERY_ITEMS)
			.map { SearchSuggestionItem.RecentQuery(it) }
	}.getOrElse { e ->
		e.printStackTraceDebug()
		listOf(SearchSuggestionItem.Text(0, e))
	}

	private suspend fun getTags(searchQuery: String): List<SearchSuggestionItem> = runCatchingCancellable {
		val tags = repository.getTagsSuggestion(searchQuery, MAX_TAGS_ITEMS, null)
		if (tags.isEmpty()) {
			emptyList()
		} else {
			listOf(SearchSuggestionItem.Tags(mapTags(tags)))
		}
	}.getOrElse { e ->
		e.printStackTraceDebug()
		listOf(SearchSuggestionItem.Text(0, e))
	}

	private suspend fun getManga(searchQuery: String): List<SearchSuggestionItem> = runCatchingCancellable {
		val manga = repository.getMangaSuggestion(searchQuery, MAX_MANGA_ITEMS, null)
		if (manga.isEmpty()) {
			emptyList()
		} else {
			listOf(SearchSuggestionItem.MangaList(manga))
		}
	}.getOrElse { e ->
		e.printStackTraceDebug()
		listOf(SearchSuggestionItem.Text(0, e))
	}

	private fun getSources(searchQuery: String, enabledSources: Set<String>): List<SearchSuggestionItem> =
		runCatchingCancellable {
			repository.getSourcesSuggestion(searchQuery, MAX_SOURCES_ITEMS)
				.map { SearchSuggestionItem.Source(it, it.name in enabledSources) }
		}.getOrElse { e ->
			e.printStackTraceDebug()
			listOf(SearchSuggestionItem.Text(0, e))
		}

	private suspend fun getRecentSources(searchQuery: String): List<SearchSuggestionItem> = if (searchQuery.isEmpty()) {
		runCatchingCancellable {
			repository.getSourcesSuggestion(MAX_SOURCES_TIPS_ITEMS)
				.map { SearchSuggestionItem.SourceTip(it) }
		}.getOrElse { e ->
			e.printStackTraceDebug()
			listOf(SearchSuggestionItem.Text(0, e))
		}
	} else {
		emptyList()
	}

	private fun mapTags(tags: List<MangaTag>): List<ChipsView.ChipModel> = tags.map { tag ->
		ChipsView.ChipModel(
			title = tag.title,
			data = tag,
		)
	}
}
