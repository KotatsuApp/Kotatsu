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
import org.koitharu.kotatsu.core.model.MangaTag
import org.koitharu.kotatsu.core.parser.RemoteMangaRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.list.ui.MangaListViewModel
import org.koitharu.kotatsu.list.ui.filter.FilterState
import org.koitharu.kotatsu.list.ui.model.*
import org.koitharu.kotatsu.utils.ext.asLiveDataDistinct

class RemoteListViewModel(
	private val repository: RemoteMangaRepository,
	settings: AppSettings
) : MangaListViewModel(settings) {

	var filter = FilterState(repository.sortOrders.firstOrNull(), emptySet())
		private set
	private val mangaList = MutableStateFlow<List<Manga>?>(null)
	private val hasNextPage = MutableStateFlow(false)
	private val listError = MutableStateFlow<Throwable?>(null)
	private var loadingJob: Job? = null
	private val headerModel = MutableStateFlow(
		ListHeader(repository.title, 0, filter.sortOrder)
	)

	override val content = combine(
		mangaList,
		createListModeFlow(),
		headerModel,
		listError,
		hasNextPage
	) { list, mode, header, error, hasNext ->
		when {
			list.isNullOrEmpty() && error != null -> listOf(error.toErrorState(canRetry = true))
			list == null -> listOf(LoadingState)
			list.isEmpty() -> createEmptyState()
			else -> {
				val result = ArrayList<ListModel>(list.size + 3)
				result += header
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
	}

	override fun onRefresh() {
		loadList(append = false)
	}

	override fun onRetry() {
		loadList(append = !mangaList.value.isNullOrEmpty())
	}

	override fun onRemoveFilterTag(tag: MangaTag) {
		val tags = filter.tags
		if (tag !in tags) {
			return
		}
		applyFilter(FilterState(filter.sortOrder, tags - tag))
	}

	fun loadNextPage() {
		if (hasNextPage.value && listError.value == null) {
			loadList(append = true)
		}
	}

	fun applyFilter(newFilter: FilterState) {
		filter = newFilter
		headerModel.value = ListHeader(repository.title, 0, newFilter.sortOrder)
		mangaList.value = null
		hasNextPage.value = false
		loadList(false)
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
					sortOrder = filter.sortOrder,
					tags = filter.tags,
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

	private fun createFilterModel(): CurrentFilterModel? {
		val tags = filter.tags
		return if (tags.isEmpty()) {
			null
		} else {
			CurrentFilterModel(tags.map { ChipsView.ChipModel(0, it.title, it) })
		}
	}

	private fun createEmptyState() = listOf(
		EmptyState(
			icon = R.drawable.ic_book_cross,
			textPrimary = R.string.nothing_found,
			textSecondary = 0,
			actionStringRes = if (filter.tags.isEmpty()) 0 else R.string.reset_filter,
		)
	)
}
