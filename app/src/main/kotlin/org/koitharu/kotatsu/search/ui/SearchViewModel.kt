package org.koitharu.kotatsu.search.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.util.ext.require
import org.koitharu.kotatsu.download.ui.worker.DownloadWorker
import org.koitharu.kotatsu.list.domain.ListExtraProvider
import org.koitharu.kotatsu.list.ui.MangaListViewModel
import org.koitharu.kotatsu.list.ui.model.EmptyState
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.model.LoadingFooter
import org.koitharu.kotatsu.list.ui.model.LoadingState
import org.koitharu.kotatsu.list.ui.model.toErrorFooter
import org.koitharu.kotatsu.list.ui.model.toErrorState
import org.koitharu.kotatsu.list.ui.model.toUi
import org.koitharu.kotatsu.parsers.model.Manga
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	repositoryFactory: MangaRepository.Factory,
	settings: AppSettings,
	private val extraProvider: ListExtraProvider,
	downloadScheduler: DownloadWorker.Scheduler,
) : MangaListViewModel(settings, downloadScheduler) {

	private val query = savedStateHandle.require<String>(SearchFragment.ARG_QUERY)
	private val repository = repositoryFactory.create(savedStateHandle.require(SearchFragment.ARG_SOURCE))
	private val mangaList = MutableStateFlow<List<Manga>?>(null)
	private val hasNextPage = MutableStateFlow(false)
	private val listError = MutableStateFlow<Throwable?>(null)
	private var loadingJob: Job? = null

	override val content = combine(
		mangaList,
		listMode,
		listError,
		hasNextPage,
	) { list, mode, error, hasNext ->
		when {
			list.isNullOrEmpty() && error != null -> listOf(error.toErrorState(canRetry = true))
			list == null -> listOf(LoadingState)
			list.isEmpty() -> listOf(
				EmptyState(
					icon = R.drawable.ic_empty_common,
					textPrimary = R.string.nothing_found,
					textSecondary = R.string.text_search_holder_secondary,
					actionStringRes = 0,
				),
			)

			else -> {
				val result = ArrayList<ListModel>(list.size + 1)
				list.toUi(result, mode, extraProvider)
				when {
					error != null -> result += error.toErrorFooter()
					hasNext -> result += LoadingFooter()
				}
				result
			}
		}
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, listOf(LoadingState))

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
					query = query,
				)
				if (!append) {
					mangaList.value = list
				} else if (list.isNotEmpty()) {
					mangaList.value = mangaList.value?.plus(list) ?: list
				}
				hasNextPage.value = list.isNotEmpty()
			} catch (e: CancellationException) {
				throw e
			} catch (e: Throwable) {
				listError.value = e
			}
		}
	}
}
