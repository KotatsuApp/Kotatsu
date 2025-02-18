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
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import org.koitharu.kotatsu.search.domain.SearchKind
import org.koitharu.kotatsu.search.domain.SearchV2Helper
import javax.inject.Inject

private const val MAX_PARALLELISM = 4

@HiltViewModel
class SearchViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	private val mangaListMapper: MangaListMapper,
	private val searchHelperFactory: SearchV2Helper.Factory,
	private val sourcesRepository: MangaSourcesRepository,
	private val historyRepository: HistoryRepository,
	private val favouritesRepository: FavouritesRepository,
) : BaseViewModel() {

	val query = savedStateHandle.get<String>(AppRouter.KEY_QUERY).orEmpty()
	val kind = savedStateHandle.get<SearchKind>(AppRouter.KEY_KIND) ?: SearchKind.SIMPLE

	private val retryCounter = MutableStateFlow(0)
	private val listData = retryCounter.flatMapLatest {
		searchImpl().withLoading().withErrorHandling()
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
	private fun searchImpl(): Flow<List<SearchResultsListModel>> = channelFlow {
		searchHistory()?.let { send(it) }
		searchFavorites()?.let { send(it) }
		searchLocal()?.let { send(it) }
		val sources = sourcesRepository.getEnabledSources()
		if (sources.isEmpty()) {
			return@channelFlow
		}
		val semaphore = Semaphore(MAX_PARALLELISM)
		sources.map { source ->
			launch {
				val item = runCatchingCancellable {
					semaphore.withPermit {
						val searchHelper = searchHelperFactory.create(source)
						searchHelper(query, kind)
					}
				}.fold(
					onSuccess = { result ->
						if (result == null || result.manga.isEmpty()) {
							null
						} else {
							val list = mangaListMapper.toListModelList(
								manga = result.manga,
								mode = ListMode.GRID,
							)
							SearchResultsListModel(
								titleResId = 0,
								source = source,
								list = list,
								error = null,
								listFilter = result.listFilter,
								sortOrder = result.sortOrder,
							)
						}
					},
					onFailure = { error ->
						error.printStackTraceDebug()
						SearchResultsListModel(0, source, null, null, emptyList(), error)
					},
				)
				if (item != null) {
					send(item)
				}
			}
		}.joinAll()
	}.runningFold<SearchResultsListModel, List<SearchResultsListModel>?>(null) { list, item -> list.orEmpty() + item }
		.filterNotNull()
		.onEmpty { emit(emptyList()) }

	private suspend fun searchHistory(): SearchResultsListModel? {
		return runCatchingCancellable {
			historyRepository.search(query, kind, Int.MAX_VALUE)
		}.fold(
			onSuccess = { result ->
				if (result.isNotEmpty()) {
					SearchResultsListModel(
						titleResId = R.string.history,
						source = UnknownMangaSource,
						list = mangaListMapper.toListModelList(manga = result, mode = ListMode.GRID),
						error = null,
						listFilter = null,
						sortOrder = null,
					)
				} else {
					null
				}
			},
			onFailure = { error ->
				SearchResultsListModel(
					titleResId = R.string.history,
					source = UnknownMangaSource,
					list = emptyList(),
					error = error,
					listFilter = null,
					sortOrder = null,
				)
			},
		)
	}

	private suspend fun searchFavorites(): SearchResultsListModel? {
		return runCatchingCancellable {
			favouritesRepository.search(query, kind, Int.MAX_VALUE)
		}.fold(
			onSuccess = { result ->
				if (result.isNotEmpty()) {
					SearchResultsListModel(
						titleResId = R.string.favourites,
						source = UnknownMangaSource,
						list = mangaListMapper.toListModelList(
							manga = result,
							mode = ListMode.GRID,
							flags = MangaListMapper.NO_FAVORITE,
						),
						error = null,
						listFilter = null,
						sortOrder = null,
					)
				} else {
					null
				}
			},
			onFailure = { error ->
				SearchResultsListModel(
					titleResId = R.string.favourites,
					source = UnknownMangaSource,
					list = emptyList(),
					error = error,
					listFilter = null,
					sortOrder = null,
				)
			},
		)
	}

	private suspend fun searchLocal(): SearchResultsListModel? {
		return runCatchingCancellable {
			searchHelperFactory.create(LocalMangaSource).invoke(query, kind)
		}.fold(
			onSuccess = { result ->
				if (!result?.manga.isNullOrEmpty()) {
					SearchResultsListModel(
						titleResId = 0,
						source = LocalMangaSource,
						list = mangaListMapper.toListModelList(
							manga = result.manga,
							mode = ListMode.GRID,
							flags = MangaListMapper.NO_SAVED,
						),
						error = null,
						listFilter = result.listFilter,
						sortOrder = result.sortOrder,
					)
				} else {
					null
				}
			},
			onFailure = { error ->
				SearchResultsListModel(
					titleResId = 0,
					source = LocalMangaSource,
					list = emptyList(),
					error = error,
					listFilter = null,
					sortOrder = null,
				)
			},
		)
	}
}
