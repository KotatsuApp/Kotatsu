package org.koitharu.kotatsu.history.ui

import android.content.Context
import android.os.Build
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.history.domain.HistoryRepository
import org.koitharu.kotatsu.list.ui.MangaListViewModel
import org.koitharu.kotatsu.list.ui.model.toGridModel
import org.koitharu.kotatsu.list.ui.model.toListDetailedModel
import org.koitharu.kotatsu.list.ui.model.toListModel
import org.koitharu.kotatsu.utils.MangaShortcut
import org.koitharu.kotatsu.utils.SingleLiveEvent
import org.koitharu.kotatsu.utils.ext.onFirst

class HistoryListViewModel(
	private val repository: HistoryRepository,
	private val context: Context //todo create ShortcutRepository
	, settings: AppSettings
) : MangaListViewModel(settings) {

	val onItemRemoved = SingleLiveEvent<Manga>()

	override val content = combine(
		repository.observeAll(),
		createListModeFlow()
	) { list, mode ->
		when (mode) {
			ListMode.LIST -> list.map { it.toListModel() }
			ListMode.DETAILED_LIST -> list.map { it.toListDetailedModel() }
			ListMode.GRID -> list.map { it.toGridModel() }
		}
	}.onEach {
		isEmptyState.postValue(it.isEmpty())
	}.onStart {
		isLoading.postValue(true)
	}.onFirst {
		isLoading.postValue(false)
	}.asLiveData(viewModelScope.coroutineContext + Dispatchers.Default)

	fun clearHistory() {
		launchLoadingJob {
			repository.clear()
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
				MangaShortcut.clearAppShortcuts(context)
			}
		}
	}

	fun removeFromHistory(manga: Manga) {
		launchJob {
			repository.delete(manga)
			onItemRemoved.call(manga)
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
				MangaShortcut(manga).removeAppShortcut(context)
			}
		}
	}

}