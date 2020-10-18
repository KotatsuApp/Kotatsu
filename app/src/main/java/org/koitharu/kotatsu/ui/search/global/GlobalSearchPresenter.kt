package org.koitharu.kotatsu.ui.search.global

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.plus
import moxy.presenterScope
import org.koin.core.component.get
import org.koitharu.kotatsu.domain.MangaSearchRepository
import org.koitharu.kotatsu.ui.base.BasePresenter
import org.koitharu.kotatsu.ui.list.MangaListView
import java.io.IOException

class GlobalSearchPresenter : BasePresenter<MangaListView<Unit>>() {

	private val repository = get<MangaSearchRepository>()

	fun startSearch(query: String) {
		viewState.onLoadingStateChanged(isLoading = true)
		var isFirstCall = true
		repository.globalSearch(query)
			.catch { e ->
				if (e is IOException) {
					viewState.onError(e)
				}
			}.filterNot { x -> x.isEmpty() }
			.onEmpty {
				viewState.onListChanged(emptyList())
				viewState.onLoadingStateChanged(isLoading = false)
			}.onCompletion {
				viewState.onListAppended(emptyList())
			}.onEach {
				if (isFirstCall) {
					isFirstCall = false
					viewState.onListChanged(it)
					viewState.onLoadingStateChanged(isLoading = false)
				} else {
					viewState.onListAppended(it)
				}
			}.launchIn(presenterScope + Dispatchers.Default)
	}

}