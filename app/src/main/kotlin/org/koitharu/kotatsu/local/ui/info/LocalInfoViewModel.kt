package org.koitharu.kotatsu.local.ui.info

import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import org.koitharu.kotatsu.core.model.parcelable.ParcelableManga
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.ui.widgets.SegmentedBarView
import org.koitharu.kotatsu.core.util.ext.computeSize
import org.koitharu.kotatsu.core.util.ext.require
import org.koitharu.kotatsu.core.util.ext.toFileOrNull
import org.koitharu.kotatsu.local.data.LocalMangaRepository
import org.koitharu.kotatsu.local.data.LocalStorageManager
import javax.inject.Inject

@HiltViewModel
class LocalInfoViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	private val localMangaRepository: LocalMangaRepository,
	private val storageManager: LocalStorageManager,
) : BaseViewModel() {

	private val manga = savedStateHandle.require<ParcelableManga>(LocalInfoDialog.ARG_MANGA).manga

	val path = MutableStateFlow<String?>(null)
	val size = MutableStateFlow(-1L)
	val availableSize = MutableStateFlow(-1L)

	init {
		launchLoadingJob(Dispatchers.Default) {
			val file = manga.url.toUri().toFileOrNull() ?: localMangaRepository.findSavedManga(manga)?.file
			requireNotNull(file)
			path.value = file.path
			size.value = file.computeSize()
			availableSize.value = storageManager.computeAvailableSize()
		}
	}
}
