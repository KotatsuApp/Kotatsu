package org.koitharu.kotatsu.list.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.observeAsFlow
import org.koitharu.kotatsu.core.prefs.observeAsLiveData
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.ui.util.ReversibleAction
import org.koitharu.kotatsu.core.util.SingleLiveEvent
import org.koitharu.kotatsu.core.util.asFlowLiveData
import org.koitharu.kotatsu.download.ui.worker.DownloadWorker
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaTag

abstract class MangaListViewModel(
	private val settings: AppSettings,
	private val downloadScheduler: DownloadWorker.Scheduler,
) : BaseViewModel() {

	abstract val content: LiveData<List<ListModel>>
	protected val listModeFlow = settings.observeAsFlow(AppSettings.KEY_LIST_MODE) { listMode }
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Lazily, settings.listMode)
	val listMode = listModeFlow.asFlowLiveData(viewModelScope.coroutineContext)
	val onActionDone = SingleLiveEvent<ReversibleAction>()
	val gridScale = settings.observeAsLiveData(
		context = viewModelScope.coroutineContext + Dispatchers.Default,
		key = AppSettings.KEY_GRID_SIZE,
		valueProducer = { gridSize / 100f },
	)
	val onDownloadStarted = SingleLiveEvent<Unit>()

	open fun onUpdateFilter(tags: Set<MangaTag>) = Unit

	abstract fun onRefresh()

	abstract fun onRetry()

	fun download(items: Set<Manga>) {
		launchJob(Dispatchers.Default) {
			downloadScheduler.schedule(items)
			onDownloadStarted.emitCall(Unit)
		}
	}
}
