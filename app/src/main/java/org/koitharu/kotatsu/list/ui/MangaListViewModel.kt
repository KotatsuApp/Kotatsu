package org.koitharu.kotatsu.list.ui

import androidx.annotation.CallSuper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.model.MangaFilter
import org.koitharu.kotatsu.core.model.MangaTag
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.list.domain.AvailableFilters
import org.koitharu.kotatsu.list.ui.filter.FilterItem
import org.koitharu.kotatsu.list.ui.filter.OnFilterChangedListener
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.utils.ext.asLiveDataDistinct

abstract class MangaListViewModel(
	private val settings: AppSettings,
) : BaseViewModel(), OnFilterChangedListener {

	abstract val content: LiveData<List<ListModel>>
	val filter = MutableLiveData<List<FilterItem>>()
	val listMode = MutableLiveData<ListMode>()
	val gridScale = settings.observe()
		.filter { it == AppSettings.KEY_GRID_SIZE }
		.map { settings.gridSize / 100f }
		.asLiveDataDistinct(viewModelScope.coroutineContext + Dispatchers.IO) {
			settings.gridSize / 100f
		}

	protected fun createListModeFlow() = settings.observe()
		.filter { it == AppSettings.KEY_LIST_MODE }
		.map { settings.listMode }
		.onStart { emit(settings.listMode) }
		.distinctUntilChanged()
		.onEach {
			if (listMode.value != it) {
				listMode.postValue(it)
			}
		}

	protected var currentFilter: MangaFilter = MangaFilter(null, emptySet())
		private set(value) {
			field = value
			onFilterChanged()
		}
	protected var availableFilters: AvailableFilters? = null
	private var filterJob: Job? = null

	final override fun onSortItemClick(item: FilterItem.Sort) {
		currentFilter = currentFilter.copy(sortOrder = item.order)
	}

	final override fun onTagItemClick(item: FilterItem.Tag) {
		val tags = if (item.isChecked) {
			currentFilter.tags - item.tag
		} else {
			currentFilter.tags + item.tag
		}
		currentFilter = currentFilter.copy(tags = tags)
	}

	fun onRemoveFilterTag(tag: MangaTag) {
		val tags = currentFilter.tags
		if (tag !in tags) {
			return
		}
		currentFilter = currentFilter.copy(tags = tags - tag)
	}

	@CallSuper
	open fun onFilterChanged() {
		val previousJob = filterJob
		filterJob = launchJob(Dispatchers.Default) {
			previousJob?.cancelAndJoin()
			filter.postValue(
				availableFilters?.run {
					val list = ArrayList<FilterItem>(size + 2)
					if (sortOrders.isNotEmpty()) {
						val selectedSort = currentFilter.sortOrder ?: sortOrders.first()
						list += FilterItem.Header(R.string.sort_order)
						sortOrders.sortedBy { it.ordinal }.mapTo(list) {
							FilterItem.Sort(it, isSelected = it == selectedSort)
						}
					}
					if (tags.isNotEmpty()) {
						list += FilterItem.Header(R.string.genres)
						tags.sortedBy { it.title }.mapTo(list) {
							FilterItem.Tag(it, isChecked = it in currentFilter.tags)
						}
					}
					ensureActive()
					list
				}.orEmpty()
			)
		}
	}

	abstract fun onRefresh()

	abstract fun onRetry()
}