package org.koitharu.kotatsu.list.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ListMode

abstract class MangaListViewModel(
	private val settings: AppSettings
) : BaseViewModel() {

	abstract val content: LiveData<List<Any>>
	val isEmptyState = MutableLiveData(false)
	val filter = MutableLiveData<MangaFilterConfig>()
	val listMode = MutableLiveData<ListMode>()
	val gridScale = settings.observe()
		.filter { it == AppSettings.KEY_GRID_SIZE }
		.map { settings.gridSize / 100f }
		.onStart { emit(settings.gridSize / 100f) }
		.asLiveData(viewModelScope.coroutineContext + Dispatchers.IO)

	protected fun createListModeFlow() = settings.observe()
		.filter { it == AppSettings.KEY_LIST_MODE }
		.map { settings.listMode }
		.onStart { emit(settings.listMode) }
		.distinctUntilChanged()
		.onEach { listMode.postValue(it) }
}