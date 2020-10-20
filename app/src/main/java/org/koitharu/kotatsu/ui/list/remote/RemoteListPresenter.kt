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
import org.koitharu.kotatsu.ui.base.BasePresenter
import org.koitharu.kotatsu.ui.list.MangaListView

@InjectViewState
class RemoteListPresenter(source: MangaSource) : BasePresenter<MangaListView<Unit>>() {

	private val repository by lazy(LazyThreadSafetyMode.PUBLICATION) {
		source.repository
	}
	private var isFilterInitialized = false
	private var filter: MangaFilter? = null

	override fun onFirstViewAttach() {
		super.onFirstViewAttach()
		loadFilter()
	}

	fun loadList(offset: Int) {
		presenterScope.launch {
			viewState.onLoadingStateChanged(true)
			try {
				val list = withContext(Dispatchers.Default) {
					repository.getList(
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
			loadFilter()
		}
	}

	fun applyFilter(filter: MangaFilter) {
		this.filter = filter
		viewState.onListChanged(emptyList())
		loadList(0)
	}

	private fun loadFilter() {
		isFilterInitialized = true
		launchJob {
			try {
				val (sorts, tags) = withContext(Dispatchers.Default) {
					repository.sortOrders.sortedBy { it.ordinal } to repository.getTags()
						.sortedBy { it.title }
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