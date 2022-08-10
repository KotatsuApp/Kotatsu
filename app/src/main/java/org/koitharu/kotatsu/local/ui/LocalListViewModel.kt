package org.koitharu.kotatsu.local.ui

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.download.ui.service.DownloadService
import org.koitharu.kotatsu.history.domain.HistoryRepository
import org.koitharu.kotatsu.list.ui.MangaListViewModel
import org.koitharu.kotatsu.list.ui.model.EmptyState
import org.koitharu.kotatsu.list.ui.model.LoadingState
import org.koitharu.kotatsu.list.ui.model.toErrorState
import org.koitharu.kotatsu.list.ui.model.toUi
import org.koitharu.kotatsu.local.domain.LocalMangaRepository
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.utils.SingleLiveEvent
import org.koitharu.kotatsu.utils.ext.asLiveDataDistinct
import org.koitharu.kotatsu.utils.ext.printStackTraceDebug

@HiltViewModel
class LocalListViewModel @Inject constructor(
	private val repository: LocalMangaRepository,
	private val historyRepository: HistoryRepository,
	settings: AppSettings,
) : MangaListViewModel(settings) {

	val onMangaRemoved = SingleLiveEvent<Unit>()
	private val listError = MutableStateFlow<Throwable?>(null)
	private val mangaList = MutableStateFlow<List<Manga>?>(null)

	override val content = combine(
		mangaList,
		createListModeFlow(),
		listError,
	) { list, mode, error ->
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
			else -> list.toUi(mode)
		}
	}.asLiveDataDistinct(viewModelScope.coroutineContext + Dispatchers.Default, listOf(LoadingState))

	init {
		onRefresh()
		cleanup()
		watchDirectories()
	}

	override fun onRefresh() {
		launchLoadingJob(Dispatchers.Default) {
			doRefresh()
		}
	}

	override fun onRetry() = onRefresh()

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
			mangaList.value = repository.getList(0, null, null)
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
}
