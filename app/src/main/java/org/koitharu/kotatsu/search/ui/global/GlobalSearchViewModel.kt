package org.koitharu.kotatsu.search.ui.global

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.list.ui.MangaListViewModel
import org.koitharu.kotatsu.list.ui.model.*
import org.koitharu.kotatsu.search.domain.MangaSearchRepository
import org.koitharu.kotatsu.utils.ext.asLiveDataDistinct
import org.koitharu.kotatsu.utils.ext.onFirst
import java.util.*

class GlobalSearchViewModel(
	private val query: String,
	private val repository: MangaSearchRepository,
	settings: AppSettings
) : MangaListViewModel(settings) {

	private val mangaList = MutableStateFlow<List<Manga>?>(null)
	private val hasNextPage = MutableStateFlow(false)
	private val listError = MutableStateFlow<Throwable?>(null)
	private var searchJob: Job? = null

	override val content = combine(
		mangaList,
		createListModeFlow(),
		listError,
		hasNextPage
	) { list, mode, error, hasNext ->
		when {
			list.isNullOrEmpty() && error != null -> listOf(error.toErrorState(canRetry = true))
			list == null -> listOf(LoadingState)
			list.isEmpty() -> listOf(EmptyState(R.string.nothing_found))
			else -> {
				val result = ArrayList<ListModel>(list.size + 1)
				list.toUi(result, mode)
				when {
					error != null -> result += error.toErrorFooter()
					hasNext -> result += LoadingFooter
				}
				result
			}
		}
	}.asLiveDataDistinct(viewModelScope.coroutineContext + Dispatchers.Default, listOf(LoadingState))

	init {
		onRefresh()
	}

	override fun onRetry() {
		onRefresh()
	}

	override fun onRefresh() {
		searchJob?.cancel()
		searchJob = repository.globalSearch(query)
			.catch { e ->
				listError.value = e
				isLoading.postValue(false)
			}.onStart {
				mangaList.value = null
				listError.value = null
				isLoading.postValue(true)
				hasNextPage.value = true
			}.onEmpty {
				mangaList.value = emptyList()
			}.onCompletion {
				isLoading.postValue(false)
				hasNextPage.value = false
			}.onFirst {
				isLoading.postValue(false)
			}.onEach {
				mangaList.value = mangaList.value?.plus(it) ?: listOf(it)
			}.launchIn(viewModelScope + Dispatchers.Default)
	}
}