package org.koitharu.kotatsu.list.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.base.ui.util.ReversibleAction
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.observeAsFlow
import org.koitharu.kotatsu.core.prefs.observeAsLiveData
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.utils.SingleLiveEvent
import org.koitharu.kotatsu.utils.asFlowLiveData

abstract class MangaListViewModel(
	private val settings: AppSettings,
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

	open fun onUpdateFilter(tags: Set<MangaTag>) = Unit

	abstract fun onRefresh()

	abstract fun onRetry()
}
