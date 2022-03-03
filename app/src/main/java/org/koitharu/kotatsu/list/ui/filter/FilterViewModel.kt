package org.koitharu.kotatsu.list.ui.filter

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.domain.MangaDataRepository
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.model.SortOrder
import org.koitharu.kotatsu.core.parser.RemoteMangaRepository
import java.util.*

class FilterViewModel(
	private val repository: RemoteMangaRepository,
	dataRepository: MangaDataRepository,
	state: FilterState,
) : BaseViewModel(), OnFilterChangedListener {

	val filter = MutableLiveData<List<FilterItem>>()
	val result = MutableLiveData<FilterState>()
	private var job: Job? = null
	private var selectedSortOrder: SortOrder? = state.sortOrder
	private val selectedTags = HashSet(state.tags)
	private val availableTagsDeferred = viewModelScope.async(Dispatchers.Default + createErrorHandler()) {
		repository.getTags()
	}
	private val localTagsDeferred = viewModelScope.async(Dispatchers.Default + createErrorHandler()) {
		dataRepository.findTags(repository.source)
	}

	init {
		showFilter()
	}

	override fun onSortItemClick(item: FilterItem.Sort) {
		selectedSortOrder = item.order
		updateFilters()
	}

	override fun onTagItemClick(item: FilterItem.Tag) {
		val isModified = if (item.isChecked) {
			selectedTags.remove(item.tag)
		} else {
			selectedTags.add(item.tag)
		}
		if (isModified) {
			updateFilters()
		}
	}

	private fun updateFilters() {
		val previousJob = job
		job = launchJob(Dispatchers.Default) {
			previousJob?.cancelAndJoin()
			val tags = availableTagsDeferred.await()
			val localTags = localTagsDeferred.await()
			val sortOrders = repository.sortOrders
			val list = ArrayList<FilterItem>(sortOrders.size + tags.size + 2)
			list.add(FilterItem.Header(R.string.sort_order))
			sortOrders.sortedBy { it.ordinal }.mapTo(list) {
				FilterItem.Sort(it, isSelected = it == selectedSortOrder)
			}
			if (tags.isNotEmpty() || selectedTags.isNotEmpty()) {
				list.add(FilterItem.Header(R.string.genres))
				val mappedTags = TreeSet<FilterItem.Tag>(compareBy({ !it.isChecked }, { it.tag.title }))
				localTags.mapTo(mappedTags) { FilterItem.Tag(it, isChecked = it in selectedTags) }
				tags.mapTo(mappedTags) { FilterItem.Tag(it, isChecked = it in selectedTags) }
				selectedTags.mapTo(mappedTags) { FilterItem.Tag(it, isChecked = true) }
				list.addAll(mappedTags)
			}
			ensureActive()
			filter.postValue(list)
		}
		result.value = FilterState(selectedSortOrder, selectedTags)
	}

	private fun showFilter() {
		job = launchJob(Dispatchers.Default) {
			val sortOrders = repository.sortOrders
			val list = ArrayList<FilterItem>(sortOrders.size + selectedTags.size + 3)
			list.add(FilterItem.Header(R.string.sort_order))
			sortOrders.sortedBy { it.ordinal }.mapTo(list) {
				FilterItem.Sort(it, isSelected = it == selectedSortOrder)
			}
			if (selectedTags.isNotEmpty()) {
				list.add(FilterItem.Header(R.string.genres))
				selectedTags.sortedBy { it.title }.mapTo(list) {
					FilterItem.Tag(it, isChecked = it in selectedTags)
				}
			}
			list.add(FilterItem.Loading)
			filter.postValue(list)
			updateFilters()
		}
	}
}