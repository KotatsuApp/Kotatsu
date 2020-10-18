package org.koitharu.kotatsu.ui.list

import moxy.viewstate.strategy.AddToEndSingleTagStrategy
import moxy.viewstate.strategy.AddToEndStrategy
import moxy.viewstate.strategy.StateStrategyType
import moxy.viewstate.strategy.alias.AddToEnd
import moxy.viewstate.strategy.alias.AddToEndSingle
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaFilter
import org.koitharu.kotatsu.core.model.MangaTag
import org.koitharu.kotatsu.core.model.SortOrder
import org.koitharu.kotatsu.ui.base.BaseMvpView

interface MangaListView<E> : BaseMvpView {

	@StateStrategyType(AddToEndSingleTagStrategy::class, tag = "content")
	fun onListChanged(list: List<Manga>)

	@StateStrategyType(AddToEndStrategy::class, tag = "content")
	fun onListAppended(list: List<Manga>)

	@StateStrategyType(AddToEndSingleTagStrategy::class, tag = "content")
	fun onListError(e: Throwable)

	@AddToEndSingle
	fun onInitFilter(sortOrders: List<SortOrder>, tags: List<MangaTag>, currentFilter: MangaFilter?)

	@AddToEnd
	fun onItemRemoved(item: Manga)
}