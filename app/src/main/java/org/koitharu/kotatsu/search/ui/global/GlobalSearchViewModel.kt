package org.koitharu.kotatsu.search.ui.global

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.list.ui.MangaListViewModel
import org.koitharu.kotatsu.search.domain.MangaSearchRepository
import org.koitharu.kotatsu.utils.ext.onFirst
import java.io.IOException

class GlobalSearchViewModel(
	private val repository: MangaSearchRepository,
	settings: AppSettings
) : MangaListViewModel(settings) {

	override val content = MutableLiveData<List<Any>>()
	private var searchJob: Job? = null

	fun startSearch(query: String) {
		isLoading.value = true
		searchJob?.cancel()
		searchJob = repository.globalSearch(query)
			.flowOn(Dispatchers.Default)
			.catch { e ->
				if (e is IOException) {
					onError.call(e)
				}
			}.filterNot { x -> x.isEmpty() }
			.onEmpty {
				content.value = emptyList()
				isLoading.value = false
			}.onCompletion {
				// TODO
			}.onFirst {
				isLoading.value = false
			}.onEach {
				content.value = content.value.orEmpty() + it
			}.launchIn(viewModelScope)
	}
}