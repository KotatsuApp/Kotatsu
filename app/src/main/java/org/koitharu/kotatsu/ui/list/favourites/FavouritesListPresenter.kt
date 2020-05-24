package org.koitharu.kotatsu.ui.list.favourites

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moxy.InjectViewState
import moxy.presenterScope
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.domain.favourites.FavouritesRepository
import org.koitharu.kotatsu.ui.common.BasePresenter
import org.koitharu.kotatsu.ui.list.MangaListView

@InjectViewState
class FavouritesListPresenter : BasePresenter<MangaListView<Unit>>() {

	private lateinit var repository: FavouritesRepository

	override fun onFirstViewAttach() {
		repository = FavouritesRepository()
		super.onFirstViewAttach()
	}

	fun loadList(categoryId: Long, offset: Int) {
		presenterScope.launch {
			viewState.onLoadingStateChanged(true)
			try {
				val list = withContext(Dispatchers.IO) {
					if (categoryId == 0L) {
						repository.getAllManga(offset = offset)
					} else {
						repository.getManga(categoryId = categoryId, offset = offset)
					}
				}
				if (offset == 0) {
					viewState.onListChanged(list)
				} else {
					viewState.onListAppended(list)
				}
			} catch (e: CancellationException) {
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
	}
}