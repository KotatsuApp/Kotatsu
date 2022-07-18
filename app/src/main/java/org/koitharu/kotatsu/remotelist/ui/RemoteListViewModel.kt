package org.koitharu.kotatsu.remotelist.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.domain.MangaDataRepository
import org.koitharu.kotatsu.base.ui.widgets.ChipsView
import org.koitharu.kotatsu.core.parser.RemoteMangaRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.list.ui.MangaListViewModel
import org.koitharu.kotatsu.list.ui.filter.FilterCoordinator
import org.koitharu.kotatsu.list.ui.filter.FilterItem
import org.koitharu.kotatsu.list.ui.filter.FilterState
import org.koitharu.kotatsu.list.ui.filter.OnFilterChangedListener
import org.koitharu.kotatsu.list.ui.model.*
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.search.domain.MangaSearchRepository
import org.koitharu.kotatsu.utils.ext.asLiveDataDistinct
import org.koitharu.kotatsu.utils.ext.printStackTraceDebug
import java.util.*

private const val FILTER_MIN_INTERVAL = 250L

class RemoteListViewModel(
	private val repository: RemoteMangaRepository,
	private val searchRepository: MangaSearchRepository,
	settings: AppSettings,
	dataRepository: MangaDataRepository,
) : MangaListViewModel(settings), OnFilterChangedListener {

	private val filter = FilterCoordinator(repository, dataRepository, viewModelScope)
	private val mangaList = MutableStateFlow<List<Manga>?>(null)
	private val hasNextPage = MutableStateFlow(false)
	private val listError = MutableStateFlow<Throwable?>(null)
	private var loadingJob: Job? = null

	val filterItems: LiveData<List<FilterItem>>
		get() = filter.items

	override val content = combine(
		mangaList,
		createListModeFlow(),
		createHeaderFlow(),
		listError,
		hasNextPage,
	) { list, mode, header, error, hasNext ->
		buildList(list?.size?.plus(2) ?: 2) {
			add(header)
			when {
				list.isNullOrEmpty() && error != null -> add(error.toErrorState(canRetry = true))
				list == null -> add(LoadingState)
				list.isEmpty() -> add(createEmptyState(header.hasSelectedTags))
				else -> {
					list.toUi(this, mode)
					when {
						error != null -> add(error.toErrorFooter())
						hasNext -> add(LoadingFooter)
					}
				}
			}
		}
	}.asLiveDataDistinct(viewModelScope.coroutineContext + Dispatchers.Default, listOf(LoadingState))

	init {
		filter.observeState()
			.debounce(FILTER_MIN_INTERVAL)
			.onEach { filterState ->
				loadingJob?.cancelAndJoin()
				mangaList.value = null
				hasNextPage.value = false
				loadList(filterState, false)
			}.catch { error ->
				listError.value = error
			}.launchIn(viewModelScope)
	}

	override fun onRefresh() {
		loadList(filter.snapshot(), append = false)
	}

	override fun onRetry() {
		loadList(filter.snapshot(), append = !mangaList.value.isNullOrEmpty())
	}

	override fun onSortItemClick(item: FilterItem.Sort) {
		filter.onSortItemClick(item)
	}

	override fun onTagItemClick(item: FilterItem.Tag) {
		filter.onTagItemClick(item)
	}

	fun loadNextPage() {
		if (hasNextPage.value && listError.value == null) {
			loadList(filter.snapshot(), append = true)
		}
	}

	fun filterSearch(query: String) = filter.performSearch(query)

	fun resetFilter() = filter.reset()

	override fun onUpdateFilter(tags: Set<MangaTag>) {
		applyFilter(tags)
	}

	fun applyFilter(tags: Set<MangaTag>) {
		filter.setTags(tags)
	}

	private fun loadList(filterState: FilterState, append: Boolean) {
		if (loadingJob?.isActive == true) {
			return
		}
		loadingJob = launchLoadingJob(Dispatchers.Default) {
			try {
				listError.value = null
				val list = repository.getList(
					offset = if (append) mangaList.value?.size ?: 0 else 0,
					sortOrder = filterState.sortOrder,
					tags = filterState.tags,
				)
				if (!append) {
					mangaList.value = list
				} else if (list.isNotEmpty()) {
					mangaList.value = mangaList.value?.plus(list) ?: list
				}
				hasNextPage.value = list.isNotEmpty()
			} catch (e: Throwable) {
				e.printStackTraceDebug()
				listError.value = e
				if (!mangaList.value.isNullOrEmpty()) {
					errorEvent.postCall(e)
				}
			}
		}
	}

	private fun createEmptyState(canResetFilter: Boolean) = EmptyState(
		icon = R.drawable.ic_empty_search,
		textPrimary = R.string.nothing_found,
		textSecondary = 0,
		actionStringRes = if (canResetFilter) R.string.reset_filter else 0,
	)

	private fun createHeaderFlow() = combine(
		filter.observeState(),
		filter.observeAvailableTags(),
	) { state, available ->
		val chips = createChipsList(state, available.orEmpty())
		ListHeader2(chips, state.sortOrder, state.tags.isNotEmpty())
	}

	private suspend fun createChipsList(
		filterState: FilterState,
		availableTags: Set<MangaTag>
	): List<ChipsView.ChipModel> {
		val selectedTags = filterState.tags.toMutableSet()
		var tags = searchRepository.getTagsSuggestion("", 6, repository.source)
		if (tags.isEmpty()) {
			tags = availableTags.take(6)
		}
		if (tags.isEmpty() && selectedTags.isEmpty()) {
			return emptyList()
		}
		val result = LinkedList<ChipsView.ChipModel>()
		for (tag in tags) {
			val model = ChipsView.ChipModel(
				icon = 0,
				title = tag.title,
				isCheckable = true,
				isChecked = selectedTags.remove(tag),
				data = tag,
			)
			if (model.isChecked) {
				result.addFirst(model)
			} else {
				result.addLast(model)
			}
		}
		for (tag in selectedTags) {
			val model = ChipsView.ChipModel(
				icon = 0,
				title = tag.title,
				isCheckable = true,
				isChecked = true,
				data = tag,
			)
			result.addFirst(model)
		}
		return result
	}
}