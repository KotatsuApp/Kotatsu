package org.koitharu.kotatsu.ui.list.remote

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moxy.InjectViewState
import moxy.presenterScope
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.core.model.MangaFilter
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.domain.MangaProviderFactory
import org.koitharu.kotatsu.ui.base.BasePresenter
import org.koitharu.kotatsu.ui.list.MangaListView

@InjectViewState
class RemoteListPresenter : BasePresenter<MangaListView<Unit>>() {

	private var isFilterInitialized = false
	private var filter: MangaFilter? = null

	fun loadList(source: MangaSource, offset: Int) {
		presenterScope.launch {
			viewState.onLoadingStateChanged(true)
			try {
				val list = withContext(Dispatchers.Default) {
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
		presenterScope.launch {
			try {
				val (sorts, tags) = withContext(Dispatchers.Default) {
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