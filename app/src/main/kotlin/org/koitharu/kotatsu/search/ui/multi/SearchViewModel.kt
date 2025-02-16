package org.koitharu.kotatsu.search.ui.multi

import androidx.annotation.CheckResult
import androidx.collection.LongSet
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.LocalMangaSource
import org.koitharu.kotatsu.core.model.UnknownMangaSource
import org.koitharu.kotatsu.core.nav.AppRouter
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import org.koitharu.kotatsu.explore.data.MangaSourcesRepository
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository
import org.koitharu.kotatsu.history.data.HistoryRepository
import org.koitharu.kotatsu.list.domain.MangaListMapper
import org.koitharu.kotatsu.list.ui.model.EmptyState
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.model.LoadingFooter
import org.koitharu.kotatsu.list.ui.model.LoadingState
import org.koitharu.kotatsu.local.data.LocalMangaRepository
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaListFilter
import org.koitharu.kotatsu.parsers.model.SortOrder
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import javax.inject.Inject

private const val MAX_PARALLELISM = 4
private const val MIN_HAS_MORE_ITEMS = 8

@HiltViewModel
class SearchViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	private val mangaListMapper: MangaListMapper,
	private val mangaRepositoryFactory: MangaRepository.Factory,
	private val sourcesRepository: MangaSourcesRepository,
	private val historyRepository: HistoryRepository,
	private val localMangaRepository: LocalMangaRepository,
	private val favouritesRepository: FavouritesRepository,
) : BaseViewModel() {

	val query = savedStateHandle.get<String>(AppRouter.KEY_QUERY).orEmpty()

	private val retryCounter = MutableStateFlow(0)
	private val listData = retryCounter.flatMapLatest {
		searchImpl(query).withLoading().withErrorHandling()
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, null)

	val list: StateFlow<List<ListModel>> = combine(
		listData.filterNotNull(),
		isLoading,
	) { list, loading ->
		when {
			list.isEmpty() -> listOf(
				when {
					loading -> LoadingState
					else -> EmptyState(
						icon = R.drawable.ic_empty_common,
						textPrimary = R.string.nothing_found,
						textSecondary = R.string.text_search_holder_secondary,
						actionStringRes = 0,
					)
				},
			)

			loading -> list + LoadingFooter()
			else -> list
		}
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, listOf(LoadingState))

	fun getItems(ids: LongSet): Set<Manga> {
		val snapshot = listData.value ?: return emptySet()
		val result = HashSet<Manga>(ids.size)
		snapshot.forEach { x ->
			for (item in x.list) {
				if (item.id in ids) {
					result.add(item.manga)
				}
			}
		}
		return result
	}

	fun retry() {
		retryCounter.value += 1
	}

	@CheckResult
	private fun searchImpl(q: String): Flow<List<SearchResultsListModel>> = channelFlow {
		searchHistory(q)?.let { send(it) }
		searchFavorites(q)?.let { send(it) }
		searchLocal(q)?.let { send(it) }
		val sources = sourcesRepository.getEnabledSources()
		if (sources.isEmpty()) {
			return@channelFlow
		}
		val semaphore = Semaphore(MAX_PARALLELISM)
		sources.mapNotNull { source ->
			val repository = mangaRepositoryFactory.create(source)
			if (!repository.filterCapabilities.isSearchSupported) {
				null
			} else {
				launch {
					val item = runCatchingCancellable {
						semaphore.withPermit {
							mangaListMapper.toListModelList(
								manga = repository.getList(offset = 0, null, MangaListFilter(query = q)),
								mode = ListMode.GRID,
							)
						}
					}.fold(
						onSuccess = { list ->
							if (list.isEmpty()) {
								null
							} else {
								SearchResultsListModel(0, source, list.size > MIN_HAS_MORE_ITEMS, list, null)
							}
						},
						onFailure = { error ->
							error.printStackTraceDebug()
							SearchResultsListModel(0, source, true, emptyList(), error)
						},
					)
					if (item != null) {
						send(item)
					}
				}
			}
		}.joinAll()
	}.runningFold<SearchResultsListModel, List<SearchResultsListModel>?>(null) { list, item -> list.orEmpty() + item }
		.filterNotNull()
		.onEmpty { emit(emptyList()) }

	private suspend fun searchHistory(q: String): SearchResultsListModel? {
		return runCatchingCancellable {
			historyRepository.search(q, Int.MAX_VALUE)
		}.fold(
			onSuccess = { result ->
				if (result.isNotEmpty()) {
					SearchResultsListModel(
						titleResId = R.string.history,
						source = UnknownMangaSource,
						hasMore = false,
						list = mangaListMapper.toListModelList(manga = result, mode = ListMode.GRID),
						error = null,
					)
				} else {
					null
				}
			},
			onFailure = { error ->
				SearchResultsListModel(
					titleResId = R.string.history,
					source = UnknownMangaSource,
					hasMore = false,
					list = emptyList(),
					error = error,
				)
			},
		)
	}

	private suspend fun searchFavorites(q: String): SearchResultsListModel? {
		return runCatchingCancellable {
			favouritesRepository.search(q, Int.MAX_VALUE)
		}.fold(
			onSuccess = { result ->
				if (result.isNotEmpty()) {
					SearchResultsListModel(
						titleResId = R.string.favourites,
						source = UnknownMangaSource,
						hasMore = false,
						list = mangaListMapper.toListModelList(manga = result, mode = ListMode.GRID),
						error = null,
					)
				} else {
					null
				}
			},
			onFailure = { error ->
				SearchResultsListModel(
					titleResId = R.string.favourites,
					source = UnknownMangaSource,
					hasMore = false,
					list = emptyList(),
					error = error,
				)
			},
		)
	}

	private suspend fun searchLocal(q: String): SearchResultsListModel? {
		return runCatchingCancellable {
			localMangaRepository.getList(0, SortOrder.RELEVANCE, MangaListFilter(query = q))
		}.fold(
			onSuccess = { result ->
				if (result.isNotEmpty()) {
					SearchResultsListModel(
						titleResId = 0,
						source = LocalMangaSource,
						hasMore = result.size > MIN_HAS_MORE_ITEMS,
						list = mangaListMapper.toListModelList(manga = result, mode = ListMode.GRID),
						error = null,
					)
				} else {
					null
				}
			},
			onFailure = { error ->
				SearchResultsListModel(
					titleResId = 0,
					source = LocalMangaSource,
					hasMore = true,
					list = emptyList(),
					error = error,
				)
			},
		)
	}
}
