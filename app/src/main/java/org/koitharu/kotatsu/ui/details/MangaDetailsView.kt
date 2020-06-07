package org.koitharu.kotatsu.ui.details

import moxy.viewstate.strategy.AddToEndSingleTagStrategy
import moxy.viewstate.strategy.AddToEndStrategy
import moxy.viewstate.strategy.StateStrategyType
import moxy.viewstate.strategy.alias.AddToEndSingle
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

	@AddToEndSingle
	fun onNewChaptersChanged(newChapters: Int)

	@StateStrategyType(AddToEndSingleTagStrategy::class, tag = "content")
	fun onListChanged(list: List<Manga>) = Unit

	@StateStrategyType(AddToEndStrategy::class, tag = "content")
	fun onListAppended(list: List<Manga>) = Unit

	@StateStrategyType(AddToEndSingleTagStrategy::class, tag = "content")
	fun onListError(e: Throwable) = Unit
}