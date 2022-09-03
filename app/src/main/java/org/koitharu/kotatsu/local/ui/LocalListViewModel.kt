package org.koitharu.kotatsu.local.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.util.*
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.widgets.ChipsView
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.download.ui.service.DownloadService
import org.koitharu.kotatsu.history.domain.HistoryRepository
import org.koitharu.kotatsu.history.domain.PROGRESS_NONE
import org.koitharu.kotatsu.list.domain.ListExtraProvider
import org.koitharu.kotatsu.list.ui.MangaListViewModel
import org.koitharu.kotatsu.list.ui.model.*
import org.koitharu.kotatsu.local.domain.LocalMangaRepository
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.parsers.model.SortOrder
import org.koitharu.kotatsu.tracker.domain.TrackingRepository
import org.koitharu.kotatsu.utils.SingleLiveEvent
import org.koitharu.kotatsu.utils.ext.asLiveDataDistinct
import org.koitharu.kotatsu.utils.ext.printStackTraceDebug

@HiltViewModel
class LocalListViewModel @Inject constructor(
	private val repository: LocalMangaRepository,
	private val historyRepository: HistoryRepository,
	private val trackingRepository: TrackingRepository,
	private val settings: AppSettings,
) : MangaListViewModel(settings), ListExtraProvider {

	val onMangaRemoved = SingleLiveEvent<Unit>()
	val sortOrder = MutableLiveData(settings.localListOrder)
	private val listError = MutableStateFlow<Throwable?>(null)
	private val mangaList = MutableStateFlow<List<Manga>?>(null)
	private val selectedTags = MutableStateFlow<Set<MangaTag>>(emptySet())
	private var refreshJob: Job? = null

	override val content = combine(
		mangaList,
		createListModeFlow(),
		sortOrder.asFlow(),
		selectedTags,
		listError,
	) { list, mode, order, tags, error ->
		when {
			error != null -> listOf(error.toErrorState(canRetry = true))
			list == null -> listOf(LoadingState)
			list.isEmpty() -> listOf(
				EmptyState(
					icon = R.drawable.ic_empty_local,
					textPrimary = R.string.text_local_holder_primary,
					textSecondary = R.string.text_local_holder_secondary,
					actionStringRes = R.string._import,
				),
			)
			else -> buildList(list.size + 1) {
				add(createHeader(list, tags, order))
				list.toUi(this, mode, this@LocalListViewModel)
			}
		}
	}.asLiveDataDistinct(viewModelScope.coroutineContext + Dispatchers.Default, listOf(LoadingState))

	init {
		onRefresh()
		cleanup()
		watchDirectories()
	}

	override fun onUpdateFilter(tags: Set<MangaTag>) {
		selectedTags.value = tags
		onRefresh()
	}

	override fun onRefresh() {
		val prevJob = refreshJob
		refreshJob = launchLoadingJob(Dispatchers.Default) {
			prevJob?.cancelAndJoin()
			doRefresh()
		}
	}

	override fun onRetry() = onRefresh()

	fun setSortOrder(value: SortOrder) {
		sortOrder.value = value
		settings.localListOrder = value
		onRefresh()
	}

	fun delete(ids: Set<Long>) {
		launchLoadingJob {
			withContext(Dispatchers.Default) {
				val itemsToRemove = checkNotNull(mangaList.value).filter { it.id in ids }
				for (manga in itemsToRemove) {
					val original = repository.getRemoteManga(manga)
					repository.delete(manga) || throw IOException("Unable to delete file")
					runCatching {
						historyRepository.deleteOrSwap(manga, original)
					}
					mangaList.update { list ->
						list?.filterNot { it.id == manga.id }
					}
				}
			}
			onMangaRemoved.call(Unit)
		}
	}

	private suspend fun doRefresh() {
		try {
			listError.value = null
			mangaList.value = repository.getList(0, selectedTags.value, sortOrder.value)
		} catch (e: Throwable) {
			listError.value = e
		}
	}

	private fun cleanup() {
		if (!DownloadService.isRunning && !ImportService.isRunning && !LocalChaptersRemoveService.isRunning) {
			viewModelScope.launch {
				runCatching {
					repository.cleanup()
				}.onFailure { error ->
					error.printStackTraceDebug()
				}
			}
		}
	}

	private fun watchDirectories() {
		viewModelScope.launch(Dispatchers.Default) {
			repository.watchReadableDirs()
				.collectLatest {
					doRefresh()
				}
		}
	}

	private fun createHeader(mangaList: List<Manga>, selectedTags: Set<MangaTag>, order: SortOrder): ListHeader2 {
		val tags = HashMap<MangaTag, Int>()
		for (item in mangaList) {
			for (tag in item.tags) {
				tags[tag] = tags[tag]?.plus(1) ?: 1
			}
		}
		val topTags = tags.entries.sortedByDescending { it.value }.take(6)
		val chips = LinkedList<ChipsView.ChipModel>()
		for ((tag, _) in topTags) {
			val model = ChipsView.ChipModel(
				icon = 0,
				title = tag.title,
				isCheckable = true,
				isChecked = tag in selectedTags,
				data = tag,
			)
			if (model.isChecked) {
				chips.addFirst(model)
			} else {
				chips.addLast(model)
			}
		}
		return ListHeader2(
			chips = chips,
			sortOrder = order,
			hasSelectedTags = selectedTags.isNotEmpty(),
		)
	}

	override suspend fun getCounter(mangaId: Long): Int {
		return trackingRepository.getNewChaptersCount(mangaId)
	}

	override suspend fun getProgress(mangaId: Long): Float {
		return if (settings.isReadingIndicatorsEnabled) {
			historyRepository.getProgress(mangaId)
		} else {
			PROGRESS_NONE
		}
	}
}
