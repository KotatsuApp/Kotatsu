package org.koitharu.kotatsu.list.ui.filter

import androidx.annotation.WorkerThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.parser.MangaDataRepository
import org.koitharu.kotatsu.core.parser.RemoteMangaRepository
import org.koitharu.kotatsu.list.ui.model.ListHeader
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.model.LoadingFooter
import org.koitharu.kotatsu.list.ui.model.LoadingState
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.parsers.util.SuspendLazy
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import org.koitharu.kotatsu.util.ext.printStackTraceDebug
import java.text.Collator
import java.util.Locale
import java.util.TreeSet

class FilterCoordinator(
	private val repository: RemoteMangaRepository,
	dataRepository: MangaDataRepository,
	private val coroutineScope: CoroutineScope,
) : OnFilterChangedListener {

	private val currentState = MutableStateFlow(FilterState(repository.defaultSortOrder, emptySet()))
	private var searchQuery = MutableStateFlow("")
	private val localTags = SuspendLazy {
		dataRepository.findTags(repository.source)
	}
	private var availableTagsDeferred = loadTagsAsync()

	val items: StateFlow<List<ListModel>> = getItemsFlow()
		.stateIn(coroutineScope + Dispatchers.Default, SharingStarted.Eagerly, listOf(LoadingState))

	init {
		observeState()
	}

	override fun onSortItemClick(item: FilterItem.Sort) {
		currentState.update { oldValue ->
			FilterState(item.order, oldValue.tags)
		}
		repository.defaultSortOrder = item.order
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

	fun observeAvailableTags(): Flow<Set<MangaTag>?> = flow {
		if (!availableTagsDeferred.isCompleted) {
			emit(emptySet())
		}
		emit(availableTagsDeferred.await())
	}

	fun observeState() = currentState.asStateFlow()

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
		val localTags = localTags.get()
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
	): List<ListModel> {
		val sortOrders = repository.sortOrders.sortedBy { it.ordinal }
		val tags = mergeTags(state.tags, allTags.tags).toList()
		val list = ArrayList<ListModel>(tags.size + sortOrders.size + 3)
		if (query.isEmpty()) {
			if (sortOrders.isNotEmpty()) {
				list.add(ListHeader(R.string.sort_order, 0, null))
				sortOrders.mapTo(list) {
					FilterItem.Sort(it, isSelected = it == state.sortOrder)
				}
			}
			if (allTags.isLoading || allTags.isError || tags.isNotEmpty()) {
				list.add(ListHeader(R.string.genres, 0, null))
				tags.mapTo(list) {
					FilterItem.Tag(it, isChecked = it in state.tags)
				}
			}
			if (allTags.isError) {
				list.add(FilterItem.Error(R.string.filter_load_error))
			} else if (allTags.isLoading) {
				list.add(LoadingFooter())
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
		runCatchingCancellable {
			repository.getTags()
		}.onFailure { error ->
			error.printStackTraceDebug()
		}.getOrNull()
	}

	private fun mergeTags(primary: Set<MangaTag>, secondary: Set<MangaTag>): Set<MangaTag> {
		val result = TreeSet(TagTitleComparator(repository.source.locale))
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
			return isError == other.isError
		}

		override fun hashCode(): Int {
			var result = tags.hashCode()
			result = 31 * result + isLoading.hashCode()
			result = 31 * result + isError.hashCode()
			return result
		}
	}

	private class TagTitleComparator(lc: String?) : Comparator<MangaTag> {

		private val collator = lc?.let { Collator.getInstance(Locale(it)) }

		override fun compare(o1: MangaTag, o2: MangaTag): Int {
			val t1 = o1.title.lowercase()
			val t2 = o2.title.lowercase()
			return collator?.compare(t1, t2) ?: compareValues(t1, t2)
		}
	}
}
