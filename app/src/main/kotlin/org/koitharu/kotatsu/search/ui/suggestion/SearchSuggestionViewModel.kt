package org.koitharu.kotatsu.search.ui.suggestion

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.SearchSuggestionType
import org.koitharu.kotatsu.core.prefs.observeAsFlow
import org.koitharu.kotatsu.core.prefs.observeAsStateFlow
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.ui.widgets.ChipsView
import org.koitharu.kotatsu.core.util.ext.sizeOrZero
import org.koitharu.kotatsu.explore.data.MangaSourcesRepository
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.search.domain.MangaSearchRepository
import org.koitharu.kotatsu.search.ui.suggestion.model.SearchSuggestionItem
import javax.inject.Inject

private const val DEBOUNCE_TIMEOUT = 500L
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
	private var suggestionJob: Job? = null
	private var invalidateOnResume = false

	val isIncognitoModeEnabled = settings.observeAsStateFlow(
		scope = viewModelScope + Dispatchers.Default,
		key = AppSettings.KEY_INCOGNITO_MODE,
		valueProducer = { isIncognitoModeEnabled },
	)

	val suggestion = MutableStateFlow<List<SearchSuggestionItem>>(emptyList())

	init {
		setupSuggestion()
	}

	fun onQueryChanged(newQuery: String) {
		query.value = newQuery
	}

	fun saveQuery(query: String) {
		if (!settings.isIncognitoModeEnabled) {
			repository.saveSearchQuery(query)
		}
		invalidateOnResume = true
	}

	fun clearSearchHistory() {
		launchJob(Dispatchers.Default) {
			repository.clearSearchHistory()
			setupSuggestion()
		}
	}

	fun onSourceToggle(source: MangaSource, isEnabled: Boolean) {
		launchJob(Dispatchers.Default) {
			sourcesRepository.setSourcesEnabled(setOf(source), isEnabled)
		}
	}

	fun onResume() {
		if (invalidateOnResume) {
			invalidateOnResume = false
			setupSuggestion()
		}
	}

	fun deleteQuery(query: String) {
		launchJob(Dispatchers.Default) {
			repository.deleteSearchQuery(query)
			setupSuggestion()
		}
	}

	private fun setupSuggestion() {
		suggestionJob?.cancel()
		suggestionJob = combine(
			query.debounce(DEBOUNCE_TIMEOUT),
			sourcesRepository.observeEnabledSources().map { it.toSet() },
			settings.observeAsFlow(AppSettings.KEY_SEARCH_SUGGESTION_TYPES) { searchSuggestionTypes },
			::Triple,
		).mapLatest { (searchQuery, enabledSources, types) ->
			buildSearchSuggestion(searchQuery, enabledSources, types)
		}.distinctUntilChanged()
			.onEach {
				suggestion.value = it
			}.withErrorHandling().launchIn(viewModelScope + Dispatchers.Default)
	}

	private suspend fun buildSearchSuggestion(
		searchQuery: String,
		enabledSources: Set<MangaSource>,
		types: Set<SearchSuggestionType>,
	): List<SearchSuggestionItem> = coroutineScope {
		val queriesDeferred = if (SearchSuggestionType.QUERIES_RECENT in types) {
			async { repository.getQuerySuggestion(searchQuery, MAX_QUERY_ITEMS) }
		} else {
			null
		}
		val hintsDeferred = if (SearchSuggestionType.QUERIES_SUGGEST in types) {
			async { repository.getQueryHintSuggestion(searchQuery, MAX_HINTS_ITEMS) }
		} else {
			null
		}
		val authorsDeferred = if (SearchSuggestionType.AUTHORS in types) {
			async { repository.getAuthorsSuggestion(searchQuery, MAX_AUTHORS_ITEMS) }
		} else {
			null
		}
		val tagsDeferred = if (SearchSuggestionType.GENRES in types) {
			async { repository.getTagsSuggestion(searchQuery, MAX_TAGS_ITEMS, null) }
		} else {
			null
		}
		val mangaDeferred = if (SearchSuggestionType.MANGA in types) {
			async { repository.getMangaSuggestion(searchQuery, MAX_MANGA_ITEMS, null) }
		} else {
			null
		}
		val sources = if (SearchSuggestionType.SOURCES in types) {
			repository.getSourcesSuggestion(searchQuery, MAX_SOURCES_ITEMS)
		} else {
			null
		}
		val sourcesTipsDeferred = if (searchQuery.isEmpty() && SearchSuggestionType.RECENT_SOURCES in types) {
			async { repository.getSourcesSuggestion(MAX_SOURCES_TIPS_ITEMS) }
		} else {
			null
		}

		val tags = tagsDeferred?.await()
		val mangaList = mangaDeferred?.await()
		val queries = queriesDeferred?.await()
		val hints = hintsDeferred?.await()
		val authors = authorsDeferred?.await()
		val sourcesTips = sourcesTipsDeferred?.await()

		buildList(queries.sizeOrZero() + sources.sizeOrZero() + authors.sizeOrZero() + hints.sizeOrZero() + 2) {
			if (!tags.isNullOrEmpty()) {
				add(SearchSuggestionItem.Tags(mapTags(tags)))
			}
			if (!mangaList.isNullOrEmpty()) {
				add(SearchSuggestionItem.MangaList(mangaList))
			}
			sources?.mapTo(this) { SearchSuggestionItem.Source(it, it in enabledSources) }
			queries?.mapTo(this) { SearchSuggestionItem.RecentQuery(it) }
			authors?.mapTo(this) { SearchSuggestionItem.Author(it) }
			hints?.mapTo(this) { SearchSuggestionItem.Hint(it) }
			sourcesTips?.mapTo(this) { SearchSuggestionItem.SourceTip(it) }
		}
	}

	private fun mapTags(tags: List<MangaTag>): List<ChipsView.ChipModel> = tags.map { tag ->
		ChipsView.ChipModel(
			title = tag.title,
			data = tag,
		)
	}
}
