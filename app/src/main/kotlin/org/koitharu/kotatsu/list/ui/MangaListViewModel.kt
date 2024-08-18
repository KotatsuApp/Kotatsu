package org.koitharu.kotatsu.list.ui

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.core.prefs.observeAsFlow
import org.koitharu.kotatsu.core.prefs.observeAsStateFlow
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.ui.util.ReversibleAction
import org.koitharu.kotatsu.core.util.ext.MutableEventFlow
import org.koitharu.kotatsu.core.util.ext.call
import org.koitharu.kotatsu.download.ui.worker.DownloadWorker
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaTag

abstract class MangaListViewModel(
	private val settings: AppSettings,
	private val downloadScheduler: DownloadWorker.Scheduler,
) : BaseViewModel() {

	abstract val content: StateFlow<List<ListModel>>
	open val listMode = settings.observeAsFlow(AppSettings.KEY_LIST_MODE) { listMode }
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, settings.listMode)
	val onActionDone = MutableEventFlow<ReversibleAction>()
	val gridScale = settings.observeAsStateFlow(
		scope = viewModelScope + Dispatchers.Default,
		key = AppSettings.KEY_GRID_SIZE,
		valueProducer = { gridSize / 100f },
	)
	val onDownloadStarted = MutableEventFlow<Unit>()

	val isIncognitoModeEnabled: Boolean
		get() = settings.isIncognitoModeEnabled

	open fun onUpdateFilter(tags: Set<MangaTag>) = Unit

	abstract fun onRefresh()

	abstract fun onRetry()

	fun download(items: Set<Manga>) {
		launchJob(Dispatchers.Default) {
			downloadScheduler.schedule(items)
			onDownloadStarted.call(Unit)
		}
	}

	fun List<Manga>.skipNsfwIfNeeded() = if (settings.isNsfwContentDisabled) {
		filterNot { it.isNsfw }
	} else {
		this
	}

	protected fun observeListModeWithTriggers(): Flow<ListMode> = combine(
		listMode,
		settings.observe().filter { key ->
			key == AppSettings.KEY_PROGRESS_INDICATORS
				|| key == AppSettings.KEY_TRACKER_ENABLED
				|| key == AppSettings.KEY_QUICK_FILTER
		}.onStart { emit("") },
	) { mode, _ ->
		mode
	}
}
