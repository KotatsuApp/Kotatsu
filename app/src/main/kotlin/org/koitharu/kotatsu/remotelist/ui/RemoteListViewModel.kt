package org.koitharu.kotatsu.remotelist.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.model.distinctById
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.util.ext.MutableEventFlow
import org.koitharu.kotatsu.core.util.ext.call
import org.koitharu.kotatsu.core.util.ext.getCauseUrl
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import org.koitharu.kotatsu.core.util.ext.sizeOrZero
import org.koitharu.kotatsu.download.ui.worker.DownloadWorker
import org.koitharu.kotatsu.explore.data.MangaSourcesRepository
import org.koitharu.kotatsu.explore.domain.ExploreRepository
import org.koitharu.kotatsu.filter.ui.FilterCoordinator
import org.koitharu.kotatsu.list.domain.MangaListMapper
import org.koitharu.kotatsu.list.ui.MangaListViewModel
import org.koitharu.kotatsu.list.ui.model.EmptyState
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.model.LoadingFooter
import org.koitharu.kotatsu.list.ui.model.LoadingState
import org.koitharu.kotatsu.list.ui.model.toErrorFooter
import org.koitharu.kotatsu.list.ui.model.toErrorState
import org.koitharu.kotatsu.parsers.model.Manga
import javax.inject.Inject

private const val FILTER_MIN_INTERVAL = 250L

@HiltViewModel
open class RemoteListViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	mangaRepositoryFactory: MangaRepository.Factory,
	final override val filterCoordinator: FilterCoordinator,
	settings: AppSettings,
	mangaListMapper: MangaListMapper,
	downloadScheduler: DownloadWorker.Scheduler,
	private val exploreRepository: ExploreRepository,
	sourcesRepository: MangaSourcesRepository,
) : MangaListViewModel(settings, downloadScheduler), FilterCoordinator.Owner {

	val source = MangaSource(savedStateHandle[RemoteListFragment.ARG_SOURCE])
	val isRandomLoading = MutableStateFlow(false)
	val onOpenManga = MutableEventFlow<Manga>()

	private val repository = mangaRepositoryFactory.create(source)
	private val mangaList = MutableStateFlow<List<Manga>?>(null)
	private val hasNextPage = MutableStateFlow(false)
	private val listError = MutableStateFlow<Throwable?>(null)
	private var loadingJob: Job? = null
	private var randomJob: Job? = null

	override val content = combine(
		mangaList.map { it?.skipNsfwIfNeeded() },
		observeListModeWithTriggers(),
		listError,
		hasNextPage,
	) { list, mode, error, hasNext ->
		buildList(list?.size?.plus(2) ?: 2) {
			when {
				list.isNullOrEmpty() && error != null -> add(
					error.toErrorState(
						canRetry = true,
						secondaryAction = if (error.getCauseUrl().isNullOrEmpty()) 0 else R.string.open_in_browser,
					),
				)

				list == null -> add(LoadingState)
				list.isEmpty() -> add(createEmptyState(canResetFilter = filterCoordinator.isFilterApplied))
				else -> {
					mangaListMapper.toListModelList(this, list, mode)
					when {
						error != null -> add(error.toErrorFooter())
						hasNext -> add(LoadingFooter())
					}
				}
			}
			onBuildList(this)
		}
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Lazily, listOf(LoadingState))

	init {
		filterCoordinator.observe()
			.debounce(FILTER_MIN_INTERVAL)
			.onEach { filterState ->
				loadingJob?.cancelAndJoin()
				mangaList.value = null
				loadList(filterState, false)
			}.catch { error ->
				listError.value = error
			}.launchIn(viewModelScope)

		launchJob(Dispatchers.Default) {
			sourcesRepository.trackUsage(source)
		}
	}

	override fun onRefresh() {
		loadList(filterCoordinator.snapshot(), append = false)
	}

	override fun onRetry() {
		loadList(filterCoordinator.snapshot(), append = !mangaList.value.isNullOrEmpty())
	}

	fun loadNextPage() {
		if (hasNextPage.value && listError.value == null) {
			loadList(filterCoordinator.snapshot(), append = true)
		}
	}

	protected fun loadList(filterState: FilterCoordinator.Snapshot, append: Boolean): Job {
		loadingJob?.let {
			if (it.isActive) return it
		}
		return launchLoadingJob(Dispatchers.Default) {
			try {
				listError.value = null
				val list = repository.getList(
					offset = if (append) mangaList.value.sizeOrZero() else 0,
					order = filterState.sortOrder,
					filter = filterState.listFilter,
				)
				val prevList = mangaList.value.orEmpty()
				if (!append) {
					mangaList.value = list.distinctById()
				} else if (list.isNotEmpty()) {
					mangaList.value = (prevList + list).distinctById()
				}
				hasNextPage.value = if (append) {
					prevList != mangaList.value
				} else {
					list.size > prevList.size || hasNextPage.value
				}
			} catch (e: CancellationException) {
				throw e
			} catch (e: Throwable) {
				e.printStackTraceDebug()
				listError.value = e
				if (!mangaList.value.isNullOrEmpty()) {
					errorEvent.call(e)
				}
				hasNextPage.value = false
			}
		}.also { loadingJob = it }
	}

	protected open fun createEmptyState(canResetFilter: Boolean) = EmptyState(
		icon = R.drawable.ic_empty_common,
		textPrimary = R.string.nothing_found,
		textSecondary = 0,
		actionStringRes = if (canResetFilter) R.string.reset_filter else 0,
	)

	protected open suspend fun onBuildList(list: MutableList<ListModel>) = Unit

	fun openRandom() {
		if (randomJob?.isActive == true) {
			return
		}
		randomJob = launchLoadingJob(Dispatchers.Default) {
			isRandomLoading.value = true
			val manga = exploreRepository.findRandomManga(source, 16)
			onOpenManga.call(manga)
			isRandomLoading.value = false
		}
	}
}
