package org.koitharu.kotatsu.ui.search

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moxy.InjectViewState
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

	fun loadList(query: String, offset: Int) {
		launch {
			viewState.onLoadingChanged(true)
			try {
				//TODO select source
				val list = withContext(Dispatchers.IO) {
					MangaProviderFactory.create(MangaSource.READMANGA_RU)
						.getList(offset, query = query)
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
}