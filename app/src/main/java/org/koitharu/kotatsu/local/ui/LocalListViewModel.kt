package org.koitharu.kotatsu.local.ui

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.os.ShortcutsRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.download.ui.service.DownloadService
import org.koitharu.kotatsu.history.domain.HistoryRepository
import org.koitharu.kotatsu.list.ui.MangaListViewModel
import org.koitharu.kotatsu.list.ui.model.*
import org.koitharu.kotatsu.local.domain.LocalMangaRepository
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.utils.SingleLiveEvent
import org.koitharu.kotatsu.utils.ext.asLiveDataDistinct
import org.koitharu.kotatsu.utils.ext.printStackTraceDebug
import org.koitharu.kotatsu.utils.progress.Progress
import java.io.IOException

class LocalListViewModel(
	private val repository: LocalMangaRepository,
	private val historyRepository: HistoryRepository,
	settings: AppSettings,
	private val shortcutsRepository: ShortcutsRepository,
) : MangaListViewModel(settings) {

	val onMangaRemoved = SingleLiveEvent<Unit>()
	val importProgress = MutableLiveData<Progress?>(null)
	private val listError = MutableStateFlow<Throwable?>(null)
	private val mangaList = MutableStateFlow<List<Manga>?>(null)
	private val headerModel = ListHeader(null, R.string.local_storage, null)
	private var importJob: Job? = null

	override val content = combine(
		mangaList,
		createListModeFlow(),
		listError
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
				)
			)
			else -> ArrayList<ListModel>(list.size + 1).apply {
				add(headerModel)
				list.toUi(this, mode)
			}
		}
	}.asLiveDataDistinct(
		viewModelScope.coroutineContext + Dispatchers.Default,
		listOf(LoadingState)
	)

	init {
		onRefresh()
		cleanup()
	}

	override fun onRefresh() {
		launchLoadingJob(Dispatchers.Default) {
			doRefresh()
		}
	}

	override fun onRetry() = onRefresh()

	fun importFiles(uris: List<Uri>) {
		val previousJob = importJob
		importJob = launchJob(Dispatchers.Default) {
			previousJob?.join()
			importProgress.postValue(Progress(0, uris.size))
			for ((i, uri) in uris.withIndex()) {
				repository.import(uri)
				importProgress.postValue(Progress(i + 1, uris.size))
				doRefresh()
			}
			importProgress.postValue(null)
		}
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
			shortcutsRepository.updateShortcuts()
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
		if (!DownloadService.isRunning) {
			viewModelScope.launch {
				runCatching {
					repository.cleanup()
				}.onFailure { error ->
					error.printStackTraceDebug()
				}
			}
		}
	}
}