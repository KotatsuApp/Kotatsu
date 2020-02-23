package org.koitharu.kotatsu.ui.main.list

import moxy.MvpView
import moxy.viewstate.strategy.*
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaFilter
import org.koitharu.kotatsu.core.model.MangaTag
import org.koitharu.kotatsu.core.model.SortOrder

interface MangaListView<E> : MvpView {

	@StateStrategyType(AddToEndSingleTagStrategy::class, tag = "content")
	fun onListChanged(list: List<Manga>)

	@StateStrategyType(AddToEndStrategy::class, tag = "content")
	fun onListAppended(list: List<Manga>)

	@StateStrategyType(AddToEndSingleStrategy::class)
	fun onLoadingChanged(isLoading: Boolean)

	@StateStrategyType(OneExecutionStateStrategy::class)
	fun onError(e: Exception)

	@StateStrategyType(AddToEndSingleStrategy::class)
	fun onInitFilter(sortOrders: List<SortOrder>, tags: List<MangaTag>, currentFilter: MangaFilter?)

	@StateStrategyType(AddToEndStrategy::class)
	fun onItemRemoved(item: Manga)
}