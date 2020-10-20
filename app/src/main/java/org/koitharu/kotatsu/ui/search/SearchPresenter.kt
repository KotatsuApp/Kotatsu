package org.koitharu.kotatsu.ui.search

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moxy.InjectViewState
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.ui.base.BasePresenter
import org.koitharu.kotatsu.ui.list.MangaListView

@InjectViewState
class SearchPresenter : BasePresenter<MangaListView<Unit>>() {

	fun loadList(source: MangaSource, query: String, offset: Int) {
		launchLoadingJob {
			val list = withContext(Dispatchers.Default) {
				source.repository.getList(offset, query = query)
			}
			if (offset == 0) {
				viewState.onListChanged(list)
			} else {
				viewState.onListAppended(list)
			}
		}
	}
}