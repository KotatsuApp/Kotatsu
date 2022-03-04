package org.koitharu.kotatsu.local.ui

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.os.ShortcutsRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.history.domain.HistoryRepository
import org.koitharu.kotatsu.list.ui.MangaListViewModel
import org.koitharu.kotatsu.list.ui.model.*
import org.koitharu.kotatsu.local.domain.LocalMangaRepository
import org.koitharu.kotatsu.utils.SingleLiveEvent
import org.koitharu.kotatsu.utils.ext.asLiveDataDistinct
import org.koitharu.kotatsu.utils.progress.Progress
import java.io.IOException

class LocalListViewModel(
	private val repository: LocalMangaRepository,
	private val historyRepository: HistoryRepository,
	settings: AppSettings,
	private val shortcutsRepository: ShortcutsRepository,
) : MangaListViewModel(settings) {

	val onMangaRemoved = SingleLiveEvent<Manga>()
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
					R.drawable.ic_storage,
					R.string.text_local_holder_primary,
					R.string.text_local_holder_secondary
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

	fun delete(manga: Manga) {
		launchJob {
			withContext(Dispatchers.Default) {
				val original = repository.getRemoteManga(manga)
				repository.delete(manga) || throw IOException("Unable to delete file")
				runCatching {
					historyRepository.deleteOrSwap(manga, original)
				}
				mangaList.value = mangaList.value?.filterNot { it.id == manga.id }
			}
			shortcutsRepository.updateShortcuts()
			onMangaRemoved.call(manga)
		}
	}

	private suspend fun doRefresh() {
		try {
			listError.value = null
			mangaList.value = repository.getList2(0)
		} catch (e: Throwable) {
			listError.value = e
		}
	}
}