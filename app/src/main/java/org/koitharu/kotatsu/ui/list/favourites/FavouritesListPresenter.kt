package org.koitharu.kotatsu.ui.list.favourites

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import moxy.InjectViewState
import moxy.presenterScope
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.domain.favourites.FavouritesRepository
import org.koitharu.kotatsu.ui.base.BasePresenter
import org.koitharu.kotatsu.ui.list.MangaListView

@InjectViewState
class FavouritesListPresenter(
	private val categoryId: Long,
	private val repository: FavouritesRepository
) : BasePresenter<MangaListView<Unit>>() {

	fun loadList(offset: Int) {
		presenterScope.launch {
			viewState.onLoadingStateChanged(true)
			try {
				val list = if (categoryId == 0L) {
					repository.getAllManga(offset = offset)
				} else {
					repository.getManga(categoryId = categoryId, offset = offset)
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

	fun removeFromFavourites(manga: Manga) {
		launchJob {
			if (categoryId == 0L) {
				repository.removeFromFavourites(manga)
			} else {
				repository.removeFromCategory(manga, categoryId)
			}
			viewState.onItemRemoved(manga)
		}
	}
}