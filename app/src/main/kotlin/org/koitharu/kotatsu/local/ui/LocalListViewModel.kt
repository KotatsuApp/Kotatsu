package org.koitharu.kotatsu.local.ui

import android.content.SharedPreferences
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.util.ext.MutableEventFlow
import org.koitharu.kotatsu.core.util.ext.call
import org.koitharu.kotatsu.core.util.ext.toFileOrNull
import org.koitharu.kotatsu.core.util.ext.toUriOrNull
import org.koitharu.kotatsu.download.ui.worker.DownloadWorker
import org.koitharu.kotatsu.explore.data.MangaSourcesRepository
import org.koitharu.kotatsu.explore.domain.ExploreRepository
import org.koitharu.kotatsu.filter.ui.FilterCoordinator
import org.koitharu.kotatsu.list.domain.MangaListMapper
import org.koitharu.kotatsu.list.ui.model.EmptyState
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.model.MangaListModel
import org.koitharu.kotatsu.list.ui.model.TipModel
import org.koitharu.kotatsu.local.data.LocalStorageChanges
import org.koitharu.kotatsu.local.data.LocalStorageManager
import org.koitharu.kotatsu.local.domain.DeleteLocalMangaUseCase
import org.koitharu.kotatsu.local.domain.model.LocalManga
import org.koitharu.kotatsu.remotelist.ui.RemoteListViewModel
import javax.inject.Inject

@HiltViewModel
class LocalListViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	mangaRepositoryFactory: MangaRepository.Factory,
	filter: FilterCoordinator,
	private val settings: AppSettings,
	downloadScheduler: DownloadWorker.Scheduler,
	mangaListMapper: MangaListMapper,
	private val deleteLocalMangaUseCase: DeleteLocalMangaUseCase,
	exploreRepository: ExploreRepository,
	@LocalStorageChanges private val localStorageChanges: SharedFlow<LocalManga?>,
	private val localStorageManager: LocalStorageManager,
	sourcesRepository: MangaSourcesRepository,
) : RemoteListViewModel(
	savedStateHandle,
	mangaRepositoryFactory,
	filter,
	settings,
	mangaListMapper,
	downloadScheduler,
	exploreRepository,
	sourcesRepository,
), SharedPreferences.OnSharedPreferenceChangeListener {

	val onMangaRemoved = MutableEventFlow<Unit>()

	init {
		launchJob(Dispatchers.Default) {
			localStorageChanges
				.collect {
					loadList(filter.snapshot(), append = false).join()
				}
		}
		settings.subscribe(this)
	}

	override suspend fun onBuildList(list: MutableList<ListModel>) {
		super.onBuildList(list)
		if (localStorageManager.hasExternalStoragePermission(isReadOnly = true)) {
			return
		}
		for (item in list) {
			if (item !is MangaListModel) {
				continue
			}
			val file = item.manga.url.toUriOrNull()?.toFileOrNull() ?: continue
			if (localStorageManager.isOnExternalStorage(file)) {
				val tip = TipModel(
					key = "permission",
					title = R.string.external_storage,
					text = R.string.missing_storage_permission,
					icon = R.drawable.ic_storage,
					primaryButtonText = R.string.fix,
					secondaryButtonText = R.string.settings,
				)
				list.add(0, tip)
				return
			}
		}
	}

	override fun onCleared() {
		settings.unsubscribe(this)
		super.onCleared()
	}

	override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
		if (key == AppSettings.KEY_LOCAL_MANGA_DIRS) {
			onRefresh()
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
