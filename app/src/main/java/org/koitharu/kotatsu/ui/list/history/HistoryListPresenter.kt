package org.koitharu.kotatsu.ui.list.history

import android.os.Build
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moxy.InjectViewState
import moxy.presenterScope
import org.koin.core.component.get
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaHistory
import org.koitharu.kotatsu.domain.history.HistoryRepository
import org.koitharu.kotatsu.ui.base.BasePresenter
import org.koitharu.kotatsu.ui.list.MangaListView
import org.koitharu.kotatsu.utils.MangaShortcut

@InjectViewState
class HistoryListPresenter : BasePresenter<MangaListView<MangaHistory>>() {

	private val repository = get<HistoryRepository>()

	fun loadList(offset: Int) {
		presenterScope.launch {
			viewState.onLoadingStateChanged(true)
			try {
				val list = withContext(Dispatchers.IO) {
					repository.getList(offset = offset)
				}
				if (offset == 0) {
					viewState.onListChanged(list)
				} else {
					viewState.onListAppended(list)
				}
			} catch (_: CancellationException) {
			} catch (e: Throwable) {
				if (BuildConfig.DEBUG) {
					e.printStackTrace()
				}
				if (offset == 0) {
					viewState.onListError(e)
				} else {
					viewState.onError(e)
				}
			} finally {
				viewState.onLoadingStateChanged(false)
			}
		}
	}

	fun clearHistory() {
		launchLoadingJob {
			repository.clear()
			viewState.onListChanged(emptyList())
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
				MangaShortcut.clearAppShortcuts(get())
			}
		}
	}

	fun removeFromHistory(manga: Manga) {
		launchJob {
			repository.delete(manga)
			viewState.onItemRemoved(manga)
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
				MangaShortcut(manga).removeAppShortcut(get())
			}
		}
	}
}