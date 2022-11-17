package org.koitharu.kotatsu.search.ui.suggestion

import androidx.lifecycle.MutableLiveData
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
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.base.ui.widgets.ChipsView
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.observeAsFlow
import org.koitharu.kotatsu.core.prefs.observeAsLiveData
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.search.domain.MangaSearchRepository
import org.koitharu.kotatsu.search.ui.suggestion.model.SearchSuggestionItem
import javax.inject.Inject

private const val DEBOUNCE_TIMEOUT = 500L
private const val MAX_MANGA_ITEMS = 6
private const val MAX_QUERY_ITEMS = 16
private const val MAX_TAGS_ITEMS = 8
private const val MAX_SOURCES_ITEMS = 6

@HiltViewModel
class SearchSuggestionViewModel @Inject constructor(
	private val repository: MangaSearchRepository,
	private val settings: AppSettings,
) : BaseViewModel() {

	private val query = MutableStateFlow("")
	private var suggestionJob: Job? = null

	val isIncognitoModeEnabled = settings.observeAsLiveData(
		context = viewModelScope.coroutineContext + Dispatchers.Default,
		key = AppSettings.KEY_INCOGNITO_MODE,
		valueProducer = { isIncognitoModeEnabled },
	)

	val suggestion = MutableLiveData<List<SearchSuggestionItem>>()

	init {
		setupSuggestion()
	}

	fun onQueryChanged(newQuery: String) {
		query.value = newQuery
	}

	fun saveQuery(query: String) {
		launchJob(Dispatchers.Default) {
			if (!settings.isIncognitoModeEnabled) {
				repository.saveSearchQuery(query)
			}
		}
	}

	fun clearSearchHistory() {
		launchJob {
			repository.clearSearchHistory()
			setupSuggestion()
		}
	}

	fun onSourceToggle(source: MangaSource, isEnabled: Boolean) {
		settings.hiddenSources = if (isEnabled) {
			settings.hiddenSources - source.name
		} else {
			settings.hiddenSources + source.name
		}
	}

	fun deleteQuery(query: String) {
		launchJob {
			repository.deleteSearchQuery(query)
			setupSuggestion()
		}
	}

	private fun setupSuggestion() {
		suggestionJob?.cancel()
		suggestionJob = combine(
			query.debounce(DEBOUNCE_TIMEOUT),
			settings.observeAsFlow(AppSettings.KEY_SOURCES_HIDDEN) { hiddenSources },
			::Pair,
		).mapLatest { (searchQuery, hiddenSources) ->
			buildSearchSuggestion(searchQuery, hiddenSources)
		}.distinctUntilChanged()
			.onEach {
				suggestion.postValue(it)
			}.launchIn(viewModelScope + Dispatchers.Default)
	}

	private suspend fun buildSearchSuggestion(
		searchQuery: String,
		hiddenSources: Set<String>,
	): List<SearchSuggestionItem> = coroutineScope {
		val queriesDeferred = async {
			repository.getQuerySuggestion(searchQuery, MAX_QUERY_ITEMS)
		}
		val tagsDeferred = async {
			repository.getTagsSuggestion(searchQuery, MAX_TAGS_ITEMS, null)
		}
		val mangaDeferred = async {
			repository.getMangaSuggestion(searchQuery, MAX_MANGA_ITEMS, null)
		}
		val sources = repository.getSourcesSuggestion(searchQuery, MAX_SOURCES_ITEMS)

		val tags = tagsDeferred.await()
		val mangaList = mangaDeferred.await()
		val queries = queriesDeferred.await()

		buildList(queries.size + sources.size + 2) {
			if (tags.isNotEmpty()) {
				add(SearchSuggestionItem.Tags(mapTags(tags)))
			}
			if (mangaList.isNotEmpty()) {
				add(SearchSuggestionItem.MangaList(mangaList))
			}
			queries.mapTo(this) { SearchSuggestionItem.RecentQuery(it) }
			sources.mapTo(this) { SearchSuggestionItem.Source(it, it.name !in hiddenSources) }
		}
	}

	private fun mapTags(tags: List<MangaTag>): List<ChipsView.ChipModel> = tags.map { tag ->
		ChipsView.ChipModel(
			icon = 0,
			title = tag.title,
			data = tag,
			isCheckable = false,
			isChecked = false,
		)
	}
}
