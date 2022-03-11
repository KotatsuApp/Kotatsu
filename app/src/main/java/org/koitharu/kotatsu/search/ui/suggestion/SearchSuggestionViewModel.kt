package org.koitharu.kotatsu.search.ui.suggestion

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.base.ui.widgets.ChipsView
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.model.MangaTag
import org.koitharu.kotatsu.search.domain.MangaSearchRepository
import org.koitharu.kotatsu.search.ui.suggestion.model.SearchSuggestionItem

private const val DEBOUNCE_TIMEOUT = 500L
private const val MAX_MANGA_ITEMS = 6
private const val MAX_QUERY_ITEMS = 16
private const val MAX_TAGS_ITEMS = 8

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
			query.debounce(DEBOUNCE_TIMEOUT),
			source,
			isLocalSearch,
			::Triple,
		).mapLatest { (searchQuery, src, srcOnly) ->
			buildSearchSuggestion(searchQuery, src, srcOnly)
		}.distinctUntilChanged()
			.onEach {
				suggestion.postValue(it)
			}.launchIn(viewModelScope + Dispatchers.Default)
	}

	private suspend fun buildSearchSuggestion(
		searchQuery: String,
		src: MangaSource?,
		srcOnly: Boolean,
	): List<SearchSuggestionItem> = coroutineScope {
		val queriesDeferred = async {
			repository.getQuerySuggestion(searchQuery, MAX_QUERY_ITEMS)
		}
		val tagsDeferred = async {
			repository.getTagsSuggestion(searchQuery, MAX_TAGS_ITEMS, src.takeIf { srcOnly })
		}
		val mangaDeferred = async {
			repository.getMangaSuggestion(searchQuery, MAX_MANGA_ITEMS, src.takeIf { srcOnly })
		}

		val tags = tagsDeferred.await()
		val mangaList = mangaDeferred.await()
		val queries = queriesDeferred.await()

		buildList(queries.size + 3) {
			if (src != null) {
				add(SearchSuggestionItem.Header(src, isLocalSearch))
			}
			if (tags.isNotEmpty()) {
				add(SearchSuggestionItem.Tags(mapTags(tags)))
			}
			if (mangaList.isNotEmpty()) {
				add(SearchSuggestionItem.MangaList(mangaList))
			}
			queries.mapTo(this) { SearchSuggestionItem.RecentQuery(it) }
		}
	}

	private fun mapTags(tags: List<MangaTag>): List<ChipsView.ChipModel> = tags.map { tag ->
		ChipsView.ChipModel(
			icon = 0,
			title = tag.title,
			data = tag,
		)
	}
}