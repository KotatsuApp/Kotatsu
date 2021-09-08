package org.koitharu.kotatsu.local.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.exceptions.UnsupportedFileException
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.os.ShortcutsRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.history.domain.HistoryRepository
import org.koitharu.kotatsu.list.ui.MangaListViewModel
import org.koitharu.kotatsu.list.ui.model.*
import org.koitharu.kotatsu.local.domain.LocalMangaRepository
import org.koitharu.kotatsu.utils.MediaStoreCompat
import org.koitharu.kotatsu.utils.SingleLiveEvent
import org.koitharu.kotatsu.utils.ext.asLiveDataDistinct
import java.io.File
import java.io.IOException

class LocalListViewModel(
	private val repository: LocalMangaRepository,
	private val historyRepository: HistoryRepository,
	private val settings: AppSettings,
	private val shortcutsRepository: ShortcutsRepository,
) : MangaListViewModel(settings) {

	val onMangaRemoved = SingleLiveEvent<Manga>()
	private val listError = MutableStateFlow<Throwable?>(null)
	private val mangaList = MutableStateFlow<List<Manga>?>(null)
	private val headerModel = ListHeader(null, R.string.local_storage)

	override val content = combine(
		mangaList,
		createListModeFlow(),
		listError
	) { list, mode, error ->
		when {
			error != null -> listOf(error.toErrorState(canRetry = true))
			list == null -> listOf(LoadingState)
			list.isEmpty() -> listOf(EmptyState(R.drawable.ic_storage, R.string.text_local_holder_primary, R.string.text_local_holder_secondary))
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
			try {
				listError.value = null
				mangaList.value = repository.getList(0, tags = null)
			} catch (e: Throwable) {
				listError.value = e
			}
		}
	}

	override fun onRetry() = onRefresh()

	fun importFile(context: Context, uri: Uri) {
		launchLoadingJob {
			val contentResolver = context.contentResolver
			withContext(Dispatchers.IO) {
				val name = MediaStoreCompat(contentResolver).getName(uri)
					?: throw IOException("Cannot fetch name from uri: $uri")
				if (!LocalMangaRepository.isFileSupported(name)) {
					throw UnsupportedFileException("Unsupported file on $uri")
				}
				val dest = settings.getStorageDir(context)?.let { File(it, name) }
					?: throw IOException("External files dir unavailable")
				@Suppress("BlockingMethodInNonBlockingContext")
				contentResolver.openInputStream(uri)?.use { source ->
					dest.outputStream().use { output ->
						source.copyTo(output)
					}
				} ?: throw IOException("Cannot open input stream: $uri")
			}
			onRefresh()
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
}