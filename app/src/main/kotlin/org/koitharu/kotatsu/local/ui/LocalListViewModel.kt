package org.koitharu.kotatsu.local.ui

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.util.ext.MutableEventFlow
import org.koitharu.kotatsu.core.util.ext.call
import org.koitharu.kotatsu.download.ui.worker.DownloadWorker
import org.koitharu.kotatsu.filter.ui.FilterCoordinator
import org.koitharu.kotatsu.list.domain.ListExtraProvider
import org.koitharu.kotatsu.list.ui.model.EmptyState
import org.koitharu.kotatsu.local.data.LocalStorageChanges
import org.koitharu.kotatsu.local.domain.DeleteLocalMangaUseCase
import org.koitharu.kotatsu.local.domain.model.LocalManga
import org.koitharu.kotatsu.remotelist.ui.RemoteListViewModel
import javax.inject.Inject

@HiltViewModel
class LocalListViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	mangaRepositoryFactory: MangaRepository.Factory,
	filter: FilterCoordinator,
	settings: AppSettings,
	downloadScheduler: DownloadWorker.Scheduler,
	listExtraProvider: ListExtraProvider,
	private val deleteLocalMangaUseCase: DeleteLocalMangaUseCase,
	@LocalStorageChanges private val localStorageChanges: SharedFlow<LocalManga?>,
) : RemoteListViewModel(
	savedStateHandle,
	mangaRepositoryFactory,
	filter,
	settings,
	listExtraProvider,
	downloadScheduler,
) {

	val onMangaRemoved = MutableEventFlow<Unit>()

	init {
		launchJob(Dispatchers.Default) {
			localStorageChanges
				.collect {
					loadList(filter.snapshot(), append = false).join()
				}
		}
	}

	fun delete(ids: Set<Long>) {
		launchLoadingJob(Dispatchers.Default) {
			deleteLocalMangaUseCase(ids)
			onMangaRemoved.call(Unit)
		}
	}

	override fun createEmptyState(canResetFilter: Boolean): EmptyState {
		return EmptyState(
			icon = R.drawable.ic_empty_local,
			textPrimary = R.string.text_local_holder_primary,
			textSecondary = R.string.text_local_holder_secondary,
			actionStringRes = R.string._import,
		)
	}
}
