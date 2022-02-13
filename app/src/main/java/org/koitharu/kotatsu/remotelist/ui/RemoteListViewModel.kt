package org.koitharu.kotatsu.remotelist.ui

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.widgets.ChipsView
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.parser.RemoteMangaRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.list.domain.AvailableFilters
import org.koitharu.kotatsu.list.ui.MangaListViewModel
import org.koitharu.kotatsu.list.ui.model.*
import org.koitharu.kotatsu.utils.ext.asLiveDataDistinct

class RemoteListViewModel(
	private val repository: MangaRepository,
	settings: AppSettings
) : MangaListViewModel(settings) {

	private val mangaList = MutableStateFlow<List<Manga>?>(null)
	private val hasNextPage = MutableStateFlow(false)
	private val listError = MutableStateFlow<Throwable?>(null)
	private var loadingJob: Job? = null
	private val headerModel = ListHeader((repository as RemoteMangaRepository).title, 0)

	override val content = combine(
		mangaList,
		createListModeFlow(),
		listError,
		hasNextPage
	) { list, mode, error, hasNext ->
		when {
			list.isNullOrEmpty() && error != null -> listOf(error.toErrorState(canRetry = true))
			list == null -> listOf(LoadingState)
			list.isEmpty() -> listOf(EmptyState(R.drawable.ic_book_cross, R.string.nothing_found, R.string._empty))
			else -> {
				val result = ArrayList<ListModel>(list.size + 3)
				result += headerModel
				createFilterModel()?.let { result.add(it) }
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
		loadList(false)
		loadFilter()
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
				val list = repository.getList2(
					offset = if (append) mangaList.value?.size ?: 0 else 0,
					sortOrder = currentFilter.sortOrder,
					tags = currentFilter.tags,
				)
				if (!append) {
					mangaList.value = list
				} else if (list.isNotEmpty()) {
					mangaList.value = mangaList.value?.plus(list) ?: list
				}
				hasNextPage.value = list.isNotEmpty()
			} catch (e: Throwable) {
				if (BuildConfig.DEBUG) {
					e.printStackTrace()
				}
				listError.value = e
			}
		}
	}

	override fun onFilterChanged() {
		super.onFilterChanged()
		mangaList.value = null
		hasNextPage.value = false
		loadList(false)
	}

	private fun createFilterModel(): CurrentFilterModel? {
		val tags = currentFilter.tags
		return if (tags.isEmpty()) {
			null
		} else {
			CurrentFilterModel(tags.map { ChipsView.ChipModel(0, it.title, it) })
		}
	}

	private fun loadFilter() {
		launchJob(Dispatchers.Default) {
			try {
				val sorts = repository.sortOrders
				val tags = repository.getTags()
				availableFilters = AvailableFilters(sorts, tags)
				onFilterChanged()
			} catch (e: Exception) {
				if (BuildConfig.DEBUG) {
					e.printStackTrace()
				}
			}
		}
	}
}