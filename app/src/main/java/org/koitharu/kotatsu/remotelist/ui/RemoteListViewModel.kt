package org.koitharu.kotatsu.remotelist.ui

import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.onEach
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaFilter
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.list.ui.MangaFilterConfig
import org.koitharu.kotatsu.list.ui.MangaListViewModel
import org.koitharu.kotatsu.list.ui.model.IndeterminateProgress
import org.koitharu.kotatsu.list.ui.model.toGridModel
import org.koitharu.kotatsu.list.ui.model.toListDetailedModel
import org.koitharu.kotatsu.list.ui.model.toListModel

class RemoteListViewModel(
	private val repository: MangaRepository,
	settings: AppSettings
) : MangaListViewModel(settings) {

	private val mangaList = MutableStateFlow<List<Manga>>(emptyList())
	private val hasNextPage = MutableStateFlow(false)
	private var appliedFilter: MangaFilter? = null
	private var loadingJob: Job? = null

	override val content = combine(mangaList.drop(1), createListModeFlow()) { list, mode ->
		when (mode) {
			ListMode.LIST -> list.map { it.toListModel() }
			ListMode.DETAILED_LIST -> list.map { it.toListDetailedModel() }
			ListMode.GRID -> list.map { it.toGridModel() }
		}
	}.onEach {
		isEmptyState.postValue(it.isEmpty())
	}.combine(hasNextPage) { list, isHasNextPage ->
		if (isHasNextPage && list.isNotEmpty()) list + IndeterminateProgress else list
	}.asLiveData(viewModelScope.coroutineContext + Dispatchers.Default)

	init {
		loadList(false)
		loadFilter()
	}

	fun loadList(append: Boolean) {
		if (loadingJob?.isActive == true) {
			return
		}
		loadingJob = launchLoadingJob(Dispatchers.Default) {
			val list = repository.getList(
				offset = if (append) mangaList.value.size else 0,
				sortOrder = appliedFilter?.sortOrder,
				tag = appliedFilter?.tag
			)
			if (!append) {
				mangaList.value = list
			} else if (list.isNotEmpty()) {
				mangaList.value += list
			}
			hasNextPage.value = list.isNotEmpty()
		}
	}

	fun applyFilter(newFilter: MangaFilter) {
		appliedFilter = newFilter
		mangaList.value = emptyList()
		hasNextPage.value = false
		loadList(false)
	}

	private fun loadFilter() {
		launchJob(Dispatchers.Default) {
			try {
				val sorts = repository.sortOrders.sortedBy { it.ordinal }
				val tags = repository.getTags().sortedBy { it.title }
				filter.postValue(MangaFilterConfig(sorts, tags, appliedFilter))
			} catch (e: Exception) {
				if (BuildConfig.DEBUG) {
					e.printStackTrace()
				}
			}
		}
	}
}