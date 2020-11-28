package org.koitharu.kotatsu.local.ui

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.core.exceptions.UnsupportedFileException
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.history.domain.HistoryRepository
import org.koitharu.kotatsu.list.ui.MangaListViewModel
import org.koitharu.kotatsu.list.ui.model.toGridModel
import org.koitharu.kotatsu.list.ui.model.toListDetailedModel
import org.koitharu.kotatsu.list.ui.model.toListModel
import org.koitharu.kotatsu.local.domain.LocalMangaRepository
import org.koitharu.kotatsu.utils.MangaShortcut
import org.koitharu.kotatsu.utils.MediaStoreCompat
import org.koitharu.kotatsu.utils.SingleLiveEvent
import org.koitharu.kotatsu.utils.ext.safe
import org.koitharu.kotatsu.utils.ext.sub
import java.io.IOException

class LocalListViewModel(
	private val repository: LocalMangaRepository,
	private val historyRepository: HistoryRepository,
	private val settings: AppSettings,
	private val context: Context
) : MangaListViewModel(settings) {

	val onMangaRemoved = SingleLiveEvent<Manga>()
	private val mangaList = MutableStateFlow<List<Manga>>(emptyList())

	override val content = combine(mangaList, createListModeFlow()) { list, mode ->
		when (mode) {
			ListMode.LIST -> list.map { it.toListModel() }
			ListMode.DETAILED_LIST -> list.map { it.toListDetailedModel() }
			ListMode.GRID -> list.map { it.toGridModel() }
		}
	}.asLiveData(viewModelScope.coroutineContext + Dispatchers.Default)

	init {
		onRefresh()
	}

	fun onRefresh() {
		launchLoadingJob {
			withContext(Dispatchers.Default) {
				val list = repository.getList(0)
				mangaList.value = list
				isEmptyState.postValue(list.isEmpty())
			}
		}
	}

	fun importFile(uri: Uri) {
		launchLoadingJob {
			val contentResolver = context.contentResolver
			withContext(Dispatchers.Default) {
				val name = MediaStoreCompat.getName(contentResolver, uri)
					?: throw IOException("Cannot fetch name from uri: $uri")
				if (!LocalMangaRepository.isFileSupported(name)) {
					throw UnsupportedFileException("Unsupported file on $uri")
				}
				val dest = settings.getStorageDir(context)?.sub(name)
					?: throw IOException("External files dir unavailable")
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
				safe {
					historyRepository.deleteOrSwap(manga, original)
				}
			}
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
				MangaShortcut(manga).removeAppShortcut(context)
			}
			onMangaRemoved.call(manga)
		}
	}
}