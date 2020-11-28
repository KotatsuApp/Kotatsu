package org.koitharu.kotatsu.search.ui.global

import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.list.ui.MangaListViewModel
import org.koitharu.kotatsu.list.ui.model.IndeterminateProgress
import org.koitharu.kotatsu.list.ui.model.toGridModel
import org.koitharu.kotatsu.list.ui.model.toListDetailedModel
import org.koitharu.kotatsu.list.ui.model.toListModel
import org.koitharu.kotatsu.search.domain.MangaSearchRepository
import org.koitharu.kotatsu.utils.ext.onFirst

class GlobalSearchViewModel(
	private val query: String,
	private val repository: MangaSearchRepository,
	settings: AppSettings
) : MangaListViewModel(settings) {

	private val mangaList = MutableStateFlow<List<Manga>>(emptyList())
	private val hasNextPage = MutableStateFlow(false)
	private var searchJob: Job? = null

	override val content = combine(mangaList.drop(1), createListModeFlow()) { list, mode ->
		when (mode) {
			ListMode.LIST -> list.map { it.toListModel() }
			ListMode.DETAILED_LIST -> list.map { it.toListDetailedModel() }
			ListMode.GRID -> list.map { it.toGridModel() }
		}
	}.combine(hasNextPage) { list, isHasNextPage ->
		if (isHasNextPage && list.isNotEmpty()) list + IndeterminateProgress else list
	}.asLiveData(viewModelScope.coroutineContext + Dispatchers.Default)

	init {
		onRefresh()
	}

	fun onRefresh() {
		searchJob?.cancel()
		searchJob = repository.globalSearch(query)
			.flowOn(Dispatchers.Default)
			.catch { e ->
				onError.postCall(e)
				isLoading.postValue(false)
				hasNextPage.value = false
			}.filterNot { x -> x.isEmpty() }
			.onStart {
				isLoading.postValue(true)
			}.onEmpty {
				mangaList.value = emptyList()
				isEmptyState.postValue(true)
				isLoading.postValue(false)
			}.onCompletion {
				isLoading.postValue(false)
				hasNextPage.value = false
			}.onFirst {
				isEmptyState.postValue(false)
				hasNextPage.value = true
				isLoading.value = false
			}.onEach {
				mangaList.value += it
			}.launchIn(viewModelScope + Dispatchers.Default)
	}
}