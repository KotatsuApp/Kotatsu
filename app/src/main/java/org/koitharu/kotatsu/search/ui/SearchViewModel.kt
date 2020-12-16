package org.koitharu.kotatsu.search.ui

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.list.ui.MangaListViewModel
import org.koitharu.kotatsu.list.ui.model.*
import org.koitharu.kotatsu.utils.ext.asLiveData
import java.util.*

class SearchViewModel(
	private val repository: MangaRepository,
	private val query: String,
	settings: AppSettings
) : MangaListViewModel(settings) {

	private val mangaList = MutableStateFlow<List<Manga>?>(null)
	private val hasNextPage = MutableStateFlow(false)
	private val listError = MutableStateFlow<Throwable?>(null)
	private var loadingJob: Job? = null

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
	}.flowOn(Dispatchers.Default).asLiveData(viewModelScope.coroutineContext, listOf(LoadingState))

	init {
		loadList(append = false)
	}

	override fun onRefresh() {
		loadList(append = false)
	}

	override fun onRetry() {
		loadList(append = !mangaList.value.isNullOrEmpty())
	}

	fun loadNextPage() {
		if (hasNextPage.value && listError.value == null) {
			loadList(append = true)
		}
	}

	private fun loadList(append: Boolean) {
		if (loadingJob?.isActive == true) {
			return
		}
		loadingJob = launchLoadingJob(Dispatchers.Default) {
			try {
				listError.value = null
				val list = repository.getList(
					offset = if (append) mangaList.value?.size ?: 0 else 0,
					query = query
				)
				if (!append) {
					mangaList.value = list
				} else if (list.isNotEmpty()) {
					mangaList.value = mangaList.value?.plus(list) ?: list
				}
				hasNextPage.value = list.isNotEmpty()
			} catch (e: Throwable) {
				listError.value = e
			}
		}
	}
}