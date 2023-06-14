package org.koitharu.kotatsu.search.ui.multi

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.plus
import kotlinx.coroutines.withTimeout
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.util.ext.MutableEventFlow
import org.koitharu.kotatsu.core.util.ext.call
import org.koitharu.kotatsu.download.ui.worker.DownloadWorker
import org.koitharu.kotatsu.list.domain.ListExtraProvider
import org.koitharu.kotatsu.list.ui.model.EmptyState
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.model.LoadingFooter
import org.koitharu.kotatsu.list.ui.model.LoadingState
import org.koitharu.kotatsu.list.ui.model.toUi
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import javax.inject.Inject

private const val MAX_PARALLELISM = 4
private const val MIN_HAS_MORE_ITEMS = 8

@HiltViewModel
class MultiSearchViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	private val extraProvider: ListExtraProvider,
	private val settings: AppSettings,
	private val mangaRepositoryFactory: MangaRepository.Factory,
	private val downloadScheduler: DownloadWorker.Scheduler,
) : BaseViewModel() {

	private var searchJob: Job? = null
	private val listData = MutableStateFlow<List<MultiSearchListModel>>(emptyList())
	private val loadingData = MutableStateFlow(false)
	val onDownloadStarted = MutableEventFlow<Unit>()

	val query = MutableStateFlow(savedStateHandle.get<String>(MultiSearchActivity.EXTRA_QUERY).orEmpty())
	val list: StateFlow<List<ListModel>> = combine(
		listData,
		loadingData,
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

	init {
		doSearch(query.value)
	}

	fun getItems(ids: Set<Long>): Set<Manga> {
		val result = HashSet<Manga>(ids.size)
		listData.value.forEach { x ->
			for (item in x.list) {
				if (item.id in ids) {
					result.add(item.manga)
				}
			}
		}
		return result
	}

	fun doSearch(q: String) {
		val prevJob = searchJob
		searchJob = launchJob(Dispatchers.Default) {
			prevJob?.cancelAndJoin()
			try {
				listData.value = emptyList()
				loadingData.value = true
				query.value = q
				searchImpl(q)
			} catch (e: CancellationException) {
				throw e
			} finally {
				loadingData.value = false
			}
		}
	}

	fun download(items: Set<Manga>) {
		launchJob(Dispatchers.Default) {
			downloadScheduler.schedule(items)
			onDownloadStarted.call(Unit)
		}
	}

	private suspend fun searchImpl(q: String) = coroutineScope {
		val sources = settings.getMangaSources(includeHidden = false)
		val dispatcher = Dispatchers.Default.limitedParallelism(MAX_PARALLELISM)
		val deferredList = sources.map { source ->
			async(dispatcher) {
				runCatchingCancellable {
					withTimeout(8_000) {
						mangaRepositoryFactory.create(source).getList(offset = 0, query = q)
							.toUi(ListMode.GRID, extraProvider)
					}
				}.fold(
					onSuccess = { list ->
						if (list.isEmpty()) {
							null
						} else {
							MultiSearchListModel(source, list.size > MIN_HAS_MORE_ITEMS, list, null)
						}
					},
					onFailure = { error ->
						error.printStackTraceDebug()
						MultiSearchListModel(source, true, emptyList(), error)
					},
				)
			}
		}
		for (deferred in deferredList) {
			deferred.await()?.let { item ->
				listData.update { x -> x + item }
			}
		}
	}
}
