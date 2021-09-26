package org.koitharu.kotatsu.list.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.model.MangaTag
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.utils.ext.asLiveDataDistinct

abstract class MangaListViewModel(
	private val settings: AppSettings
) : BaseViewModel() {

	abstract val content: LiveData<List<ListModel>>
	val filter = MutableLiveData<MangaFilterConfig>()
	val listMode = MutableLiveData<ListMode>()
	val gridScale = settings.observe()
		.filter { it == AppSettings.KEY_GRID_SIZE }
		.map { settings.gridSize / 100f }
		.asLiveDataDistinct(viewModelScope.coroutineContext + Dispatchers.IO) {
			settings.gridSize / 100f
		}

	protected fun createListModeFlow() = settings.observe()
		.filter { it == AppSettings.KEY_LIST_MODE }
		.map { settings.listMode }
		.onStart { emit(settings.listMode) }
		.distinctUntilChanged()
		.onEach {
			if (listMode.value != it) {
				listMode.postValue(it)
			}
		}

	open fun onRemoveFilterTag(tag: MangaTag) = Unit

	abstract fun onRefresh()

	abstract fun onRetry()
}