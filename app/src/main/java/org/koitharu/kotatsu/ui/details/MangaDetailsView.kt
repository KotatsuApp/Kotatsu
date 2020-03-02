package org.koitharu.kotatsu.ui.details

import moxy.MvpView
import moxy.viewstate.strategy.alias.AddToEndSingle
import moxy.viewstate.strategy.alias.OneExecution
import moxy.viewstate.strategy.alias.SingleState
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaHistory
import org.koitharu.kotatsu.ui.common.BaseMvpView

interface MangaDetailsView : BaseMvpView {

	@AddToEndSingle
	fun onMangaUpdated(manga: Manga)

	@AddToEndSingle
	fun onHistoryChanged(history: MangaHistory?)

	@AddToEndSingle
	fun onFavouriteChanged(categories: List<FavouriteCategory>)

	@SingleState
	fun onMangaRemoved(manga: Manga)
}