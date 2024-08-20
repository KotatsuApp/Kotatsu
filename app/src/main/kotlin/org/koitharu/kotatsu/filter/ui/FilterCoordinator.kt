package org.koitharu.kotatsu.filter.ui

import android.view.View
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.ViewModelLifecycle
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.parser.MangaDataRepository
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.ui.widgets.ChipsView
import org.koitharu.kotatsu.core.util.LocaleComparator
import org.koitharu.kotatsu.core.util.ext.asArrayList
import org.koitharu.kotatsu.core.util.ext.lifecycleScope
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import org.koitharu.kotatsu.filter.ui.model.FilterHeaderModel
import org.koitharu.kotatsu.filter.ui.model.FilterProperty
import org.koitharu.kotatsu.filter.ui.model.TagCatalogItem
import org.koitharu.kotatsu.list.ui.model.ErrorFooter
import org.koitharu.kotatsu.list.ui.model.ListHeader
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.model.LoadingFooter
import org.koitharu.kotatsu.list.ui.model.LoadingState
import org.koitharu.kotatsu.list.ui.model.toErrorFooter
import org.koitharu.kotatsu.parsers.model.ContentRating
import org.koitharu.kotatsu.parsers.model.MangaListFilter
import org.koitharu.kotatsu.parsers.model.MangaParserSource
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
	private val repository = mangaRepositoryFactory.create(MangaSource(savedStateHandle[RemoteListFragment.ARG_SOURCE]))
	private val currentState = MutableStateFlow(
		MangaListFilter.Advanced(
			sortOrder = repository.defaultSortOrder,
			tags = emptySet(),
			tagsExclude = emptySet(),
			locale = null,
			states = emptySet(),
			contentRating = emptySet(),
		),
	)
	private val localTags = SuspendLazy {
		dataRepository.findTags(repository.source)
	}
	private val tagsFlow = flow {
		val localTags = localTags.get()
		emit(PendingData(localTags, isLoading = true, error = null))
		tryLoadTags()
			.onSuccess { remoteTags ->
				emit(PendingData(mergeTags(remoteTags, localTags), isLoading = false, error = null))
			}.onFailure {
				emit(PendingData(localTags, isLoading = false, error = it))
			}
	}.stateIn(coroutineScope, SharingStarted.WhileSubscribed(5000), PendingData(emptySet(), true, null))
	private var availableTagsDeferred = loadTagsAsync()
	private var availableLocalesDeferred = loadLocalesAsync()
	private var allTagsLoadJob: Job? = null

	override val allTags = MutableStateFlow<List<ListModel>>(listOf(LoadingState))
		get() {
			if (allTagsLoadJob == null || field.value.any { it is ErrorFooter }) {
				loadAllTags()
			}
			return field
		}

	override val filterTags: StateFlow<FilterProperty<MangaTag>> = combine(
		currentState.distinctUntilChangedBy { it.tags },
		getTopTagsAsFlow(currentState.map { it.tags }, 16),
	) { state, tags ->
		FilterProperty(
			availableItems = tags.items.asArrayList(),
			selectedItems = state.tags,
			isLoading = tags.isLoading,
			error = tags.error,
		)
	}.stateIn(coroutineScope + Dispatchers.Default, SharingStarted.Lazily, loadingProperty())

	override val filterTagsExcluded: StateFlow<FilterProperty<MangaTag>> = if (repository.isTagsExclusionSupported) {
		combine(
			currentState.distinctUntilChangedBy { it.tagsExclude },
			getBottomTagsAsFlow(4),
		) { state, tags ->
			FilterProperty(
				availableItems = tags.items.asArrayList(),
				selectedItems = state.tagsExclude,
				isLoading = tags.isLoading,
				error = tags.error,
			)
		}.stateIn(coroutineScope + Dispatchers.Default, SharingStarted.Lazily, loadingProperty())
	} else {
		MutableStateFlow(emptyProperty())
	}

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

	override val filterContentRating: StateFlow<FilterProperty<ContentRating>> = combine(
		currentState.distinctUntilChangedBy { it.contentRating },
		flowOf(repository.contentRatings),
	) { rating, ratings ->
		FilterProperty(
			availableItems = ratings.sortedBy { it.ordinal },
			selectedItems = rating.contentRating,
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
			isFilterApplied = false,
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
			oldValue.copy(
				tags = newTags,
				tagsExclude = oldValue.tagsExclude - newTags,
			)
		}
	}

	override fun setTagExcluded(value: MangaTag, addOrRemove: Boolean) {
		currentState.update { oldValue ->
			val newTags = if (repository.isMultipleTagsSupported) {
				if (addOrRemove) {
					oldValue.tagsExclude + value
				} else {
					oldValue.tagsExclude - value
				}
			} else {
				if (addOrRemove) {
					setOf(value)
				} else {
					emptySet()
				}
			}
			oldValue.copy(
				tagsExclude = newTags,
				tags = oldValue.tags - newTags,
			)
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

	override fun setContentRating(value: ContentRating, addOrRemove: Boolean) {
		currentState.update { oldValue ->
			val newRating = if (addOrRemove) {
				oldValue.contentRating + value
			} else {
				oldValue.contentRating - value
			}
			oldValue.copy(contentRating = newRating)
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
			oldValue.copy(
				tags = tags,
				tagsExclude = oldValue.tagsExclude - tags,
			)
		}
	}

	fun reset() {
		currentState.update { oldValue ->
			MangaListFilter.Advanced.Builder(oldValue.sortOrder).build()
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
			isFilterApplied = !state.isEmpty(),
		)
	}

	private fun getLocalesAsFlow(): Flow<PendingData<Locale>> = flow {
		emit(PendingData(emptySet(), isLoading = true, error = null))
		tryLoadLocales()
			.onSuccess { locales ->
				emit(PendingData(locales, isLoading = false, error = null))
			}.onFailure {
				emit(PendingData(emptySet(), isLoading = false, error = it))
			}
	}

	private fun getTopTagsAsFlow(selectedTags: Flow<Set<MangaTag>>, limit: Int): Flow<PendingData<MangaTag>> = combine(
		selectedTags.map {
			if (it.isEmpty()) {
				searchRepository.getTagsSuggestion("", limit, repository.source)
			} else {
				searchRepository.getTagsSuggestion(it).take(limit)
			}
		},
		tagsFlow,
	) { suggested, all ->
		val res = suggested.toMutableList()
		if (res.size < limit) {
			res.addAll(all.items.shuffled().take(limit - res.size))
		}
		PendingData(res, all.isLoading, all.error.takeIf { res.size < limit })
	}

	private fun getBottomTagsAsFlow(limit: Int): Flow<PendingData<MangaTag>> = combine(
		flow { emit(searchRepository.getRareTags(repository.source, limit)) },
		tagsFlow,
	) { suggested, all ->
		val res = suggested.toMutableList()
		if (res.size < limit) {
			res.addAll(all.items.shuffled().take(limit - res.size))
		}
		PendingData(res, all.isLoading, all.error.takeIf { res.size < limit })
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
				title = tag.title,
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
				title = tag.title,
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
		val result = TreeSet(TagTitleComparator((repository.source as? MangaParserSource)?.locale))
		result.addAll(secondary)
		result.addAll(primary)
		return result
	}

	private fun loadAllTags() {
		val prevJob = allTagsLoadJob
		allTagsLoadJob = coroutineScope.launch(Dispatchers.Default) {
			runCatchingCancellable {
				prevJob?.cancelAndJoin()
				appendTagsList(localTags.get(), isLoading = true)
				appendTagsList(availableTagsDeferred.await().getOrThrow(), isLoading = false)
			}.onFailure { e ->
				allTags.value = allTags.value.filterIsInstance<TagCatalogItem>() + e.toErrorFooter()
			}
		}
	}

	private fun appendTagsList(newTags: Collection<MangaTag>, isLoading: Boolean) = allTags.update { oldList ->
		val oldTags = oldList.filterIsInstance<TagCatalogItem>()
		buildList(oldTags.size + newTags.size + if (isLoading) 1 else 0) {
			addAll(oldTags)
			newTags.mapTo(this) { TagCatalogItem(it, isChecked = false) }
			val tempSet = HashSet<MangaTag>(size)
			removeAll { x -> x is TagCatalogItem && !tempSet.add(x.tag) }
			sortBy { (it as TagCatalogItem).tag.title }
			if (isLoading) {
				add(LoadingFooter())
			}
		}
	}

	private data class PendingData<T>(
		val items: Collection<T>,
		val isLoading: Boolean,
		val error: Throwable?,
	)

	private fun <T> loadingProperty() = FilterProperty<T>(emptyList(), emptySet(), true, null)

	private fun <T> emptyProperty() = FilterProperty<T>(emptyList(), emptySet(), false, null)

	private class TagTitleComparator(lc: String?) : Comparator<MangaTag> {

		private val collator = lc?.let { Collator.getInstance(Locale(it)) }

		override fun compare(o1: MangaTag, o2: MangaTag): Int {
			val t1 = o1.title.lowercase()
			val t2 = o2.title.lowercase()
			return collator?.compare(t1, t2) ?: compareValues(t1, t2)
		}
	}
}
