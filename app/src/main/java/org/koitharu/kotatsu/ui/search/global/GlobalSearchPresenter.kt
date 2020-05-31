package org.koitharu.kotatsu.ui.search.global

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import moxy.presenterScope
import org.koitharu.kotatsu.domain.MangaSearchRepository
import org.koitharu.kotatsu.ui.common.BasePresenter
import org.koitharu.kotatsu.ui.list.MangaListView
import java.io.IOException

class GlobalSearchPresenter : BasePresenter<MangaListView<Unit>>() {

	private lateinit var repository: MangaSearchRepository

	override fun onFirstViewAttach() {
		repository = MangaSearchRepository()
		super.onFirstViewAttach()
	}

	@Suppress("EXPERIMENTAL_API_USAGE")
	fun startSearch(query: String) {
		presenterScope.launch {
			viewState.onLoadingStateChanged(isLoading = true)
			var isFirstCall = true
			repository.globalSearch(query)
				.flowOn(Dispatchers.IO)
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
				}.collect {
					if (isFirstCall) {
						isFirstCall = false
						viewState.onListChanged(it)
						viewState.onLoadingStateChanged(isLoading = false)
					} else {
						viewState.onListAppended(it)
					}
				}
		}
	}

}