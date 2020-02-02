package org.koitharu.kotatsu.ui.main.list.history

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moxy.InjectViewState
import okhttp3.internal.closeQuietly
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.core.model.MangaHistory
import org.koitharu.kotatsu.domain.HistoryRepository
import org.koitharu.kotatsu.ui.common.BasePresenter
import org.koitharu.kotatsu.ui.main.list.MangaListView

@InjectViewState
class HistoryListPresenter : BasePresenter<MangaListView<MangaHistory>>() {

	private lateinit var repository: HistoryRepository

	override fun onFirstViewAttach() {
		repository = HistoryRepository()
		super.onFirstViewAttach()
	}

	fun loadList(offset: Int) {
		launch {
			viewState.onLoadingChanged(true)
			try {
				val list = withContext(Dispatchers.IO) {
					repository.getHistory(offset = offset)
				}
				if (offset == 0) {
					viewState.onListChanged(list)
				} else {
					viewState.onListAppended(list)
				}
			} catch (e: Exception) {
				if (BuildConfig.DEBUG) {
					e.printStackTrace()
				}
				viewState.onError(e)
			} finally {
				viewState.onLoadingChanged(false)
			}
		}
	}

	fun clearHistory() {
		launch {
			viewState.onLoadingChanged(true)
			try {
				withContext(Dispatchers.IO) {
					repository.clear()
				}
				viewState.onListChanged(emptyList())
			} catch (e: Exception) {
				if (BuildConfig.DEBUG) {
					e.printStackTrace()
				}
				viewState.onError(e)
			} finally {
				viewState.onLoadingChanged(false)
			}
		}
	}

	override fun onDestroy() {
		repository.closeQuietly()
		super.onDestroy()
	}
}