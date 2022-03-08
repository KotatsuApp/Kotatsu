package org.koitharu.kotatsu.search.ui.suggestion

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.search.domain.MangaSearchRepository
import org.koitharu.kotatsu.search.ui.suggestion.model.SearchSuggestionItem

private const val DEBOUNCE_TIMEOUT = 500L
private const val SEARCH_THRESHOLD = 3
private const val MAX_MANGA_ITEMS = 3
private const val MAX_QUERY_ITEMS = 120
private const val MAX_SUGGESTION_ITEMS = MAX_MANGA_ITEMS + MAX_QUERY_ITEMS + 1

class SearchSuggestionViewModel(
	private val repository: MangaSearchRepository,
) : BaseViewModel() {

	private val query = MutableStateFlow("")
	private val source = MutableStateFlow<MangaSource?>(null)
	private val isLocalSearch = MutableStateFlow(false)
	private var suggestionJob: Job? = null

	val suggestion = MutableLiveData<List<SearchSuggestionItem>>()

	init {
		setupSuggestion()
	}

	fun onQueryChanged(newQuery: String) {
		query.value = newQuery
	}

	fun onSourceChanged(newSource: MangaSource?) {
		source.value = newSource
	}

	fun saveQuery(query: String) {
		repository.saveSearchQuery(query)
	}

	fun getLocalSearchSource(): MangaSource? {
		return source.value?.takeIf { isLocalSearch.value }
	}

	fun clearSearchHistory() {
		launchJob {
			repository.clearSearchHistory()
			setupSuggestion()
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
			query
				.debounce(DEBOUNCE_TIMEOUT)
				.mapLatest { q ->
					q to repository.getQuerySuggestion(q, MAX_QUERY_ITEMS)
				},
			source,
			isLocalSearch
		) { (q, queries), src, srcOnly ->
			val result = ArrayList<SearchSuggestionItem>(MAX_SUGGESTION_ITEMS)
			if (src != null) {
				result += SearchSuggestionItem.Header(src, isLocalSearch)
			}
			if (q.length >= SEARCH_THRESHOLD) {
				repository.getMangaSuggestion(q, MAX_MANGA_ITEMS, src.takeIf { srcOnly })
					.mapTo(result) {
						SearchSuggestionItem.MangaItem(it)
					}
			}
			queries.mapTo(result) { SearchSuggestionItem.RecentQuery(it) }
			result
		}.onEach {
			suggestion.postValue(it)
		}.launchIn(viewModelScope + Dispatchers.Default)
	}
}