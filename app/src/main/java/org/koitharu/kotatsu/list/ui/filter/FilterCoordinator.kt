package org.koitharu.kotatsu.list.ui.filter

import androidx.annotation.WorkerThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.domain.MangaDataRepository
import org.koitharu.kotatsu.core.model.MangaTag
import org.koitharu.kotatsu.core.parser.RemoteMangaRepository
import org.koitharu.kotatsu.utils.ext.asLiveDataDistinct
import java.util.*

class FilterCoordinator(
	private val repository: RemoteMangaRepository,
	dataRepository: MangaDataRepository,
	private val coroutineScope: CoroutineScope,
) : OnFilterChangedListener {

	private val currentState = MutableStateFlow(FilterState(repository.sortOrders.firstOrNull(), emptySet()))
	private var searchQuery = MutableStateFlow("")
	private val localTagsDeferred = coroutineScope.async(Dispatchers.Default, CoroutineStart.LAZY) {
		dataRepository.findTags(repository.source)
	}
	private var availableTagsDeferred = loadTagsAsync()

	val items = getItemsFlow()
		.asLiveDataDistinct(coroutineScope.coroutineContext + Dispatchers.Default)

	init {
		observeState()
	}

	override fun onSortItemClick(item: FilterItem.Sort) {
		currentState.update { oldValue ->
			FilterState(item.order, oldValue.tags)
		}
	}

	override fun onTagItemClick(item: FilterItem.Tag) {
		currentState.update { oldValue ->
			val newTags = if (item.isChecked) {
				oldValue.tags - item.tag
			} else {
				oldValue.tags + item.tag
			}
			FilterState(oldValue.sortOrder, newTags)
		}
	}

	fun observeState() = currentState.asStateFlow()

	fun removeTag(tag: MangaTag) {
		currentState.update { oldValue ->
			FilterState(oldValue.sortOrder, oldValue.tags - tag)
		}
	}

	fun setTags(tags: Set<MangaTag>) {
		currentState.update { oldValue ->
			FilterState(oldValue.sortOrder, tags)
		}
	}

	fun reset() {
		currentState.update { oldValue ->
			FilterState(oldValue.sortOrder, emptySet())
		}
	}

	fun snapshot() = currentState.value

	fun performSearch(query: String) {
		searchQuery.value = query
	}

	private fun getItemsFlow() = combine(
		getTagsAsFlow(),
		currentState,
		searchQuery,
	) { tags, state, query ->
		buildFilterList(tags, state, query)
	}

	private fun getTagsAsFlow() = flow {
		val localTags = localTagsDeferred.await()
		emit(TagsWrapper(localTags, isLoading = true, isError = false))
		val remoteTags = tryLoadTags()
		if (remoteTags == null) {
			emit(TagsWrapper(localTags, isLoading = false, isError = true))
		} else {
			emit(TagsWrapper(mergeTags(remoteTags, localTags), isLoading = false, isError = false))
		}
	}

	@WorkerThread
	private fun buildFilterList(
		allTags: TagsWrapper,
		state: FilterState,
		query: String,
	): List<FilterItem> {
		val sortOrders = repository.sortOrders.sortedBy { it.ordinal }
		val tags = mergeTags(state.tags, allTags.tags).sortedBy { it.title }
		val list = ArrayList<FilterItem>(tags.size + sortOrders.size + 3)
		if (query.isEmpty()) {
			if (sortOrders.isNotEmpty()) {
				list.add(FilterItem.Header(R.string.sort_order, 0))
				sortOrders.mapTo(list) {
					FilterItem.Sort(it, isSelected = it == state.sortOrder)
				}
			}
			if(allTags.isLoading || allTags.isError || tags.isNotEmpty()) {
				list.add(FilterItem.Header(R.string.genres, state.tags.size))
				tags.mapTo(list) {
					FilterItem.Tag(it, isChecked = it in state.tags)
				}
			}
			if (allTags.isError) {
				list.add(FilterItem.Error(R.string.filter_load_error))
			} else if (allTags.isLoading) {
				list.add(FilterItem.Loading)
			}
		} else {
			tags.mapNotNullTo(list) {
				if (it.title.contains(query, ignoreCase = true)) {
					FilterItem.Tag(it, isChecked = it in state.tags)
				} else {
					null
				}
			}
			if (list.isEmpty()) {
				list.add(FilterItem.Error(R.string.nothing_found))
			}
		}
		return list
	}

	private suspend fun tryLoadTags(): Set<MangaTag>? {
		val shouldRetryOnError = availableTagsDeferred.isCompleted
		val result = availableTagsDeferred.await()
		if (result == null && shouldRetryOnError) {
			availableTagsDeferred = loadTagsAsync()
			return availableTagsDeferred.await()
		}
		return result
	}

	private fun loadTagsAsync() = coroutineScope.async(Dispatchers.Default, CoroutineStart.LAZY) {
		runCatching {
			repository.getTags()
		}.onFailure { error ->
			if (BuildConfig.DEBUG) {
				error.printStackTrace()
			}
		}.getOrNull()
	}

	private fun mergeTags(primary: Set<MangaTag>, secondary: Set<MangaTag>): Set<MangaTag> {
		val result = TreeSet(TagTitleComparator())
		result.addAll(secondary)
		result.addAll(primary)
		return result
	}

	private class TagsWrapper(
		val tags: Set<MangaTag>,
		val isLoading: Boolean,
		val isError: Boolean,
	) {

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as TagsWrapper

			if (tags != other.tags) return false
			if (isLoading != other.isLoading) return false
			if (isError != other.isError) return false

			return true
		}

		override fun hashCode(): Int {
			var result = tags.hashCode()
			result = 31 * result + isLoading.hashCode()
			result = 31 * result + isError.hashCode()
			return result
		}
	}

	private class TagTitleComparator : Comparator<MangaTag> {

		override fun compare(o1: MangaTag, o2: MangaTag) = compareValues(
			o1.title.lowercase(),
			o2.title.lowercase(),
		)
	}
}