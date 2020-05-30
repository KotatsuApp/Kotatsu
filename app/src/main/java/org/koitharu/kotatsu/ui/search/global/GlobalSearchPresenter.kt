package org.koitharu.kotatsu.ui.search.global

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.launch
import moxy.presenterScope
import org.koitharu.kotatsu.domain.MangaSearchRepository
import org.koitharu.kotatsu.ui.common.BasePresenter
import org.koitharu.kotatsu.ui.list.MangaListView
import org.koitharu.kotatsu.utils.ext.onFirst
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
			repository.globalSearch(query)
				.flowOn(Dispatchers.IO)
				.catch { e ->
					if (e is IOException) {
						viewState.onError(e)
					}
				}
				.onFirst {
					viewState.onListChanged(emptyList())
					viewState.onLoadingStateChanged(isLoading = false)
				}
				.onEmpty {
					viewState.onListChanged(emptyList())
					viewState.onLoadingStateChanged(isLoading = false)
				}
				.collect {
					viewState.onListAppended(it)
				}
		}
	}

}