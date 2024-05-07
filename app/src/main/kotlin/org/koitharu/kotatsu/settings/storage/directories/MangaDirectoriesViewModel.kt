package org.koitharu.kotatsu.settings.storage.directories

import android.net.Uri
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.local.data.LocalStorageManager
import org.koitharu.kotatsu.settings.storage.DirectoryModel
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MangaDirectoriesViewModel @Inject constructor(
	private val storageManager: LocalStorageManager,
	private val settings: AppSettings,
) : BaseViewModel() {

	val items = MutableStateFlow(emptyList<DirectoryModel>())
	private var loadingJob: Job? = null

	init {
		loadList()
	}

	fun updateList() {
		loadList()
	}

	fun onCustomDirectoryPicked(uri: Uri) {
		launchLoadingJob(Dispatchers.Default) {
			loadingJob?.cancelAndJoin()
			storageManager.takePermissions(uri)
			val dir = requireNotNull(storageManager.resolveUri(uri)) {
				"Cannot resolve file name of \"$uri\""
			}
			if (!dir.canRead()) {
				throw AccessDeniedException(dir)
			}
			if (dir !in storageManager.getApplicationStorageDirs()) {
				settings.userSpecifiedMangaDirectories += dir
				loadList()
			}
		}
	}

	fun onRemoveClick(directory: File) {
		settings.userSpecifiedMangaDirectories -= directory
		if (settings.mangaStorageDir == directory) {
			settings.mangaStorageDir = null
		}
		loadList()
	}

	private fun loadList() {
		val prevJob = loadingJob
		loadingJob = launchJob(Dispatchers.Default) {
			prevJob?.cancelAndJoin()
			val downloadDir = storageManager.getDefaultWriteableDir()
			val applicationDirs = storageManager.getApplicationStorageDirs()
			val customDirs = settings.userSpecifiedMangaDirectories - applicationDirs
			items.value = buildList(applicationDirs.size + customDirs.size) {
				applicationDirs.mapTo(this) { dir ->
					DirectoryModel(
						title = storageManager.getDirectoryDisplayName(dir, isFullPath = false),
						titleRes = 0,
						file = dir,
						isChecked = dir == downloadDir,
						isAvailable = dir.canRead() && dir.canWrite(),
						isRemovable = false,
					)
				}
				customDirs.mapTo(this) { dir ->
					DirectoryModel(
						title = storageManager.getDirectoryDisplayName(dir, isFullPath = false),
						titleRes = 0,
						file = dir,
						isChecked = dir == downloadDir,
						isAvailable = dir.canRead() && dir.canWrite(),
						isRemovable = true,
					)
				}
			}
		}
	}
}
