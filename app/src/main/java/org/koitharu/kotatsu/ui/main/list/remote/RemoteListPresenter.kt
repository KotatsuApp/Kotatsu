package org.koitharu.kotatsu.ui.main.list.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moxy.InjectViewState
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.core.model.MangaFilter
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.domain.MangaProviderFactory
import org.koitharu.kotatsu.ui.common.BasePresenter
import org.koitharu.kotatsu.ui.main.list.MangaListView

@InjectViewState
class RemoteListPresenter : BasePresenter<MangaListView<Unit>>() {

	private var isFilterInitialized = false
	private var filter: MangaFilter? = null

	fun loadList(source: MangaSource, offset: Int) {
		launch {
			viewState.onLoadingChanged(true)
			try {
				val list = withContext(Dispatchers.IO) {
					MangaProviderFactory.create(source).getList(
						offset = offset,
						sortOrder = filter?.sortOrder,
						tag = filter?.tag
					)
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
		if (!isFilterInitialized) {
			loadFilter(source)
		}
	}

	fun applyFilter(source: MangaSource, filter: MangaFilter) {
		this.filter = filter
		viewState.onListChanged(emptyList())
		loadList(source, 0)
	}

	private fun loadFilter(source: MangaSource) {
		isFilterInitialized = true
		launch {
			try {
				val (sorts, tags) = withContext(Dispatchers.IO) {
					val repo = MangaProviderFactory.create(source)
					repo.sortOrders.sortedBy { it.ordinal } to repo.getTags().sortedBy { it.title }
				}
				viewState.onInitFilter(sorts, tags, filter)
			} catch (e: Exception) {
				if (BuildConfig.DEBUG) {
					e.printStackTrace()
				}
				isFilterInitialized = false
			}
		}
	}
}