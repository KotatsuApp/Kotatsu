package org.koitharu.kotatsu.remotelist.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.domain.MangaDataRepository
import org.koitharu.kotatsu.base.ui.widgets.ChipsView
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.parser.MangaTagHighlighter
import org.koitharu.kotatsu.core.parser.RemoteMangaRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.list.ui.MangaListViewModel
import org.koitharu.kotatsu.list.ui.filter.FilterCoordinator
import org.koitharu.kotatsu.list.ui.filter.FilterItem
import org.koitharu.kotatsu.list.ui.filter.FilterState
import org.koitharu.kotatsu.list.ui.filter.OnFilterChangedListener
import org.koitharu.kotatsu.list.ui.model.EmptyState
import org.koitharu.kotatsu.list.ui.model.ListHeader2
import org.koitharu.kotatsu.list.ui.model.LoadingFooter
import org.koitharu.kotatsu.list.ui.model.LoadingState
import org.koitharu.kotatsu.list.ui.model.toErrorFooter
import org.koitharu.kotatsu.list.ui.model.toErrorState
import org.koitharu.kotatsu.list.ui.model.toUi
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.search.domain.MangaSearchRepository
import org.koitharu.kotatsu.utils.asFlowLiveData
import org.koitharu.kotatsu.utils.ext.printStackTraceDebug
import org.koitharu.kotatsu.utils.ext.require
import java.util.LinkedList
import javax.inject.Inject

private const val FILTER_MIN_INTERVAL = 250L

@HiltViewModel
class RemoteListViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	mangaRepositoryFactory: MangaRepository.Factory,
	private val searchRepository: MangaSearchRepository,
	settings: AppSettings,
	dataRepository: MangaDataRepository,
	private val tagHighlighter: MangaTagHighlighter,
) : MangaListViewModel(settings), OnFilterChangedListener {

	val source = savedStateHandle.require<MangaSource>(RemoteListFragment.ARG_SOURCE)
	private val repository = mangaRepositoryFactory.create(source) as RemoteMangaRepository
	private val filter = FilterCoordinator(repository, dataRepository, viewModelScope)
	private val mangaList = MutableStateFlow<List<Manga>?>(null)
	private val hasNextPage = MutableStateFlow(false)
	private val listError = MutableStateFlow<Throwable?>(null)
	private var loadingJob: Job? = null

	val filterItems: LiveData<List<FilterItem>>
		get() = filter.items

	override val content = combine(
		mangaList,
		listModeFlow,
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
					list.toUi(this, mode, tagHighlighter)
					when {
						error != null -> add(error.toErrorFooter())
						hasNext -> add(LoadingFooter)
					}
				}
			}
		}
	}.asFlowLiveData(viewModelScope.coroutineContext + Dispatchers.Default, listOf(LoadingState))

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
			} catch (e: CancellationException) {
				throw e
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
		icon = R.drawable.ic_empty_common,
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
		availableTags: Set<MangaTag>,
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
				tint = 0,
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
				tint = 0,
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
