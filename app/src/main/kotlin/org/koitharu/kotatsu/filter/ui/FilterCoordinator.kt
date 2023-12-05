package org.koitharu.kotatsu.filter.ui

import android.view.View
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.ViewModelLifecycle
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.parser.MangaDataRepository
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.ui.widgets.ChipsView
import org.koitharu.kotatsu.core.util.LocaleComparator
import org.koitharu.kotatsu.core.util.ext.lifecycleScope
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import org.koitharu.kotatsu.core.util.ext.require
import org.koitharu.kotatsu.filter.ui.model.FilterHeaderModel
import org.koitharu.kotatsu.filter.ui.model.FilterProperty
import org.koitharu.kotatsu.list.ui.model.ListHeader
import org.koitharu.kotatsu.parsers.model.MangaListFilter
import org.koitharu.kotatsu.parsers.model.MangaState
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.parsers.model.SortOrder
import org.koitharu.kotatsu.parsers.util.SuspendLazy
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import org.koitharu.kotatsu.remotelist.ui.RemoteListFragment
import org.koitharu.kotatsu.search.domain.MangaSearchRepository
import java.text.Collator
import java.util.LinkedList
import java.util.Locale
import java.util.TreeSet
import javax.inject.Inject

@ViewModelScoped
class FilterCoordinator @Inject constructor(
	savedStateHandle: SavedStateHandle,
	mangaRepositoryFactory: MangaRepository.Factory,
	dataRepository: MangaDataRepository,
	private val searchRepository: MangaSearchRepository,
	lifecycle: ViewModelLifecycle,
) : MangaFilter {

	private val coroutineScope = lifecycle.lifecycleScope
	private val repository = mangaRepositoryFactory.create(savedStateHandle.require(RemoteListFragment.ARG_SOURCE))
	private val currentState = MutableStateFlow(
		MangaListFilter.Advanced(repository.defaultSortOrder, emptySet(), null, emptySet()),
	)
	private val localTags = SuspendLazy {
		dataRepository.findTags(repository.source)
	}
	private var availableTagsDeferred = loadTagsAsync()
	private var availableLocalesDeferred = loadLocalesAsync()

	override val filterTags: StateFlow<FilterProperty<MangaTag>> = combine(
		currentState.distinctUntilChangedBy { it.tags },
		getTagsAsFlow(),
	) { state, tags ->
		FilterProperty(
			availableItems = tags.items.sortedBy { it.title },
			selectedItems = state.tags,
			isLoading = tags.isLoading,
			error = tags.error,
		)
	}.stateIn(coroutineScope + Dispatchers.Default, SharingStarted.Lazily, loadingProperty())

	override val filterSortOrder: StateFlow<FilterProperty<SortOrder>> = combine(
		currentState.distinctUntilChangedBy { it.sortOrder },
		flowOf(repository.sortOrders),
	) { state, orders ->
		FilterProperty(
			availableItems = orders.sortedBy { it.ordinal },
			selectedItems = setOf(state.sortOrder),
			isLoading = false,
			error = null,
		)
	}.stateIn(coroutineScope + Dispatchers.Default, SharingStarted.Lazily, loadingProperty())

	override val filterState: StateFlow<FilterProperty<MangaState>> = combine(
		currentState.distinctUntilChangedBy { it.states },
		flowOf(repository.states),
	) { state, states ->
		FilterProperty(
			availableItems = states.sortedBy { it.ordinal },
			selectedItems = state.states,
			isLoading = false,
			error = null,
		)
	}.stateIn(coroutineScope + Dispatchers.Default, SharingStarted.Lazily, loadingProperty())

	override val filterLocale: StateFlow<FilterProperty<Locale?>> = combine(
		currentState.distinctUntilChangedBy { it.locale },
		getLocalesAsFlow(),
	) { state, locales ->
		val list = if (locales.items.isNotEmpty()) {
			val l = ArrayList<Locale?>(locales.items.size + 1)
			l.add(null)
			l.addAll(locales.items)
			try {
				l.sortWith(nullsFirst(LocaleComparator()))
			} catch (e: IllegalArgumentException) {
				e.printStackTraceDebug()
			}
			l
		} else {
			emptyList()
		}
		FilterProperty(
			availableItems = list,
			selectedItems = setOf(state.locale),
			isLoading = locales.isLoading,
			error = locales.error,
		)
	}.stateIn(coroutineScope + Dispatchers.Default, SharingStarted.Lazily, loadingProperty())

	override val header: StateFlow<FilterHeaderModel> = getHeaderFlow().stateIn(
		scope = coroutineScope + Dispatchers.Default,
		started = SharingStarted.Lazily,
		initialValue = FilterHeaderModel(
			chips = emptyList(),
			sortOrder = repository.defaultSortOrder,
			hasSelectedTags = false,
			allowMultipleTags = repository.isMultipleTagsSupported,
		),
	)

	override fun applyFilter(tags: Set<MangaTag>) {
		setTags(tags)
	}

	override fun setSortOrder(value: SortOrder) {
		currentState.update { oldValue ->
			oldValue.copy(sortOrder = value)
		}
		repository.defaultSortOrder = value
	}

	override fun setLanguage(value: Locale?) {
		currentState.update { oldValue ->
			oldValue.copy(locale = value)
		}
	}

	override fun setTag(value: MangaTag, addOrRemove: Boolean) {
		currentState.update { oldValue ->
			val newTags = if (repository.isMultipleTagsSupported) {
				if (addOrRemove) {
					oldValue.tags + value
				} else {
					oldValue.tags - value
				}
			} else {
				if (addOrRemove) {
					setOf(value)
				} else {
					emptySet()
				}
			}
			oldValue.copy(tags = newTags)
		}
	}

	override fun setState(value: MangaState, addOrRemove: Boolean) {
		currentState.update { oldValue ->
			val newStates = if (addOrRemove) {
				oldValue.states + value
			} else {
				oldValue.states - value
			}
			oldValue.copy(states = newStates)
		}
	}

	override fun onListHeaderClick(item: ListHeader, view: View) {
		currentState.update { oldValue ->
			oldValue.copy(
				sortOrder = oldValue.sortOrder,
				tags = if (item.payload == R.string.genres) emptySet() else oldValue.tags,
				locale = if (item.payload == R.string.language) null else oldValue.locale,
				states = if (item.payload == R.string.state) emptySet() else oldValue.states,
			)
		}
	}

	fun observeAvailableTags(): Flow<Set<MangaTag>?> = flow {
		if (!availableTagsDeferred.isCompleted) {
			emit(emptySet())
		}
		emit(availableTagsDeferred.await().getOrNull())
	}

	fun observeState() = currentState.asStateFlow()

	fun setTags(tags: Set<MangaTag>) {
		currentState.update { oldValue ->
			oldValue.copy(tags = tags)
		}
	}

	fun reset() {
		currentState.update { oldValue ->
			oldValue.copy(oldValue.sortOrder, emptySet(), null, emptySet())
		}
	}

	fun snapshot() = currentState.value

	private fun getHeaderFlow() = combine(
		observeState(),
		observeAvailableTags(),
	) { state, available ->
		val chips = createChipsList(state, available.orEmpty(), 8)
		FilterHeaderModel(
			chips = chips,
			sortOrder = state.sortOrder,
			hasSelectedTags = state.tags.isNotEmpty(),
			allowMultipleTags = repository.isMultipleTagsSupported,
		)
	}

	private fun getTagsAsFlow() = flow {
		val localTags = localTags.get()
		emit(PendingSet(localTags, isLoading = true, error = null))
		tryLoadTags()
			.onSuccess { remoteTags ->
				emit(PendingSet(mergeTags(remoteTags, localTags), isLoading = false, error = null))
			}.onFailure {
				emit(PendingSet(localTags, isLoading = false, error = it))
			}
	}

	private fun getLocalesAsFlow(): Flow<PendingSet<Locale>> = flow {
		emit(PendingSet(emptySet(), isLoading = true, error = null))
		tryLoadLocales()
			.onSuccess { locales ->
				emit(PendingSet(locales, isLoading = false, error = null))
			}.onFailure {
				emit(PendingSet(emptySet(), isLoading = false, error = it))
			}
	}

	private suspend fun createChipsList(
		filterState: MangaListFilter.Advanced,
		availableTags: Set<MangaTag>,
		limit: Int,
	): List<ChipsView.ChipModel> {
		val selectedTags = filterState.tags.toMutableSet()
		var tags = if (selectedTags.isEmpty()) {
			searchRepository.getTagsSuggestion("", limit, repository.source)
		} else {
			searchRepository.getTagsSuggestion(selectedTags).take(limit)
		}
		if (tags.size < limit) {
			tags = tags + availableTags.take(limit - tags.size)
		}
		if (tags.isEmpty() && selectedTags.isEmpty()) {
			return emptyList()
		}
		val result = LinkedList<ChipsView.ChipModel>()
		for (tag in tags) {
			val model = ChipsView.ChipModel(
				tint = 0,
				title = tag.title,
				icon = 0,
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
				icon = 0,
				isCheckable = true,
				isChecked = true,
				data = tag,
			)
			result.addFirst(model)
		}
		return result
	}

	private suspend fun tryLoadTags(): Result<Set<MangaTag>> {
		val shouldRetryOnError = availableTagsDeferred.isCompleted
		val result = availableTagsDeferred.await()
		if (result.isFailure && shouldRetryOnError) {
			availableTagsDeferred = loadTagsAsync()
			return availableTagsDeferred.await()
		}
		return result
	}

	private suspend fun tryLoadLocales(): Result<Set<Locale>> {
		val shouldRetryOnError = availableLocalesDeferred.isCompleted
		val result = availableLocalesDeferred.await()
		if (result.isFailure && shouldRetryOnError) {
			availableLocalesDeferred = loadLocalesAsync()
			return availableLocalesDeferred.await()
		}
		return result
	}

	private fun loadTagsAsync() = coroutineScope.async(Dispatchers.Default, CoroutineStart.LAZY) {
		runCatchingCancellable {
			repository.getTags()
		}.onFailure { error ->
			error.printStackTraceDebug()
		}
	}

	private fun loadLocalesAsync() = coroutineScope.async(Dispatchers.Default, CoroutineStart.LAZY) {
		runCatchingCancellable {
			repository.getLocales()
		}.onFailure { error ->
			error.printStackTraceDebug()
		}
	}

	private fun mergeTags(primary: Set<MangaTag>, secondary: Set<MangaTag>): Set<MangaTag> {
		val result = TreeSet(TagTitleComparator(repository.source.locale))
		result.addAll(secondary)
		result.addAll(primary)
		return result
	}

	private data class PendingSet<T>(
		val items: Set<T>,
		val isLoading: Boolean,
		val error: Throwable?,
	)

	private fun <T> loadingProperty() = FilterProperty<T>(emptyList(), emptySet(), true, null)

	private class TagTitleComparator(lc: String?) : Comparator<MangaTag> {

		private val collator = lc?.let { Collator.getInstance(Locale(it)) }

		override fun compare(o1: MangaTag, o2: MangaTag): Int {
			val t1 = o1.title.lowercase()
			val t2 = o2.title.lowercase()
			return collator?.compare(t1, t2) ?: compareValues(t1, t2)
		}
	}
}
