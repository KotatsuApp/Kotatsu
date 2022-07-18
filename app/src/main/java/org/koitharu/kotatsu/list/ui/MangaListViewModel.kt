package org.koitharu.kotatsu.list.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.onEach
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.core.prefs.observeAsFlow
import org.koitharu.kotatsu.core.prefs.observeAsLiveData
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.parsers.model.MangaTag

abstract class MangaListViewModel(
	private val settings: AppSettings,
) : BaseViewModel() {

	abstract val content: LiveData<List<ListModel>>
	val listMode = MutableLiveData<ListMode>()
	val gridScale = settings.observeAsLiveData(
		context = viewModelScope.coroutineContext + Dispatchers.Default,
		key = AppSettings.KEY_GRID_SIZE,
		valueProducer = { gridSize / 100f },
	)

	open fun onUpdateFilter(tags: Set<MangaTag>) = Unit

	protected fun createListModeFlow() = settings.observeAsFlow(AppSettings.KEY_LIST_MODE) { listMode }
		.onEach {
			if (listMode.value != it) {
				listMode.postValue(it)
			}
		}

	abstract fun onRefresh()

	abstract fun onRetry()
}