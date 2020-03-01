package org.koitharu.kotatsu.ui.details

import moxy.MvpView
import moxy.viewstate.strategy.alias.AddToEndSingle
import moxy.viewstate.strategy.alias.OneExecution
import moxy.viewstate.strategy.alias.SingleState
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaHistory

interface MangaDetailsView : MvpView {

	@AddToEndSingle
	fun onMangaUpdated(manga: Manga)

	@AddToEndSingle
	fun onLoadingStateChanged(isLoading: Boolean)

	@OneExecution
	fun onError(e: Throwable)

	@AddToEndSingle
	fun onHistoryChanged(history: MangaHistory?)

	@AddToEndSingle
	fun onFavouriteChanged(categories: List<FavouriteCategory>)

	@SingleState
	fun onMangaRemoved(manga: Manga)
}