package org.koitharu.kotatsu.ui.search

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moxy.InjectViewState
import moxy.presenterScope
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.domain.MangaProviderFactory
import org.koitharu.kotatsu.ui.common.BasePresenter
import org.koitharu.kotatsu.ui.main.list.MangaListView

@InjectViewState
class SearchPresenter : BasePresenter<MangaListView<Unit>>() {

	private lateinit var sources: Array<MangaSource>

	override fun onFirstViewAttach() {
		sources = MangaSource.values()
		super.onFirstViewAttach()
	}

	fun loadList(source: MangaSource, query: String, offset: Int) {
		presenterScope.launch {
			viewState.onLoadingStateChanged(true)
			try {
				val list = withContext(Dispatchers.IO) {
					MangaProviderFactory.create(source)
						.getList(offset, query = query)
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
				viewState.onError(e)
			} finally {
				viewState.onLoadingStateChanged(false)
			}
		}
	}
}