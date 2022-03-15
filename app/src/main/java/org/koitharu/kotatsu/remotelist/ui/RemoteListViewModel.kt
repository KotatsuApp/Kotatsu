package org.koitharu.kotatsu.remotelist.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.*
import org.koitharu.kotatsu.BuildConfig
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
import org.koitharu.kotatsu.utils.ext.asLiveDataDistinct

private const val FILTER_MIN_INTERVAL = 750L

class RemoteListViewModel(
	private val repository: RemoteMangaRepository,
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
		filter.observeState(),
		listError,
		hasNextPage,
	) { list, mode, filterState, error, hasNext ->
		buildList(list?.size?.plus(3) ?: 3) {
			add(ListHeader(repository.source.title, 0, filterState.sortOrder))
			createFilterModel(filterState)?.let { add(it) }
			when {
				list.isNullOrEmpty() && error != null -> add(error.toErrorState(canRetry = true))
				list == null -> add(LoadingState)
				list.isEmpty() -> add(createEmptyState(filterState))
				else -> {
					list.toUi(this, mode)
					when {
						error != null -> add(error.toErrorFooter())
						hasNext -> add(LoadingFooter)
					}
				}
			}
		}
	}.asLiveDataDistinct(
		viewModelScope.coroutineContext + Dispatchers.Default,
		listOf(ListHeader(repository.source.title, 0, null), LoadingState),
	)

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

	override fun onRemoveFilterTag(tag: MangaTag) {
		filter.removeTag(tag)
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
				if (BuildConfig.DEBUG) {
					e.printStackTrace()
				}
				listError.value = e
			}
		}
	}

	private fun createFilterModel(filterState: FilterState): CurrentFilterModel? {
		return if (filterState.tags.isEmpty()) {
			null
		} else {
			CurrentFilterModel(filterState.tags.map { ChipsView.ChipModel(0, it.title, it) })
		}
	}

	private fun createEmptyState(filterState: FilterState) = EmptyState(
		icon = R.drawable.ic_book_cross,
		textPrimary = R.string.nothing_found,
		textSecondary = 0,
		actionStringRes = if (filterState.tags.isEmpty()) 0 else R.string.reset_filter,
	)
}
