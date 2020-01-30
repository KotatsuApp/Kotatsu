package org.koitharu.kotatsu.ui.main.list

import moxy.MvpView
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.AddToEndSingleTagStrategy
import moxy.viewstate.strategy.AddToEndStrategy
import moxy.viewstate.strategy.StateStrategyType
import org.koitharu.kotatsu.core.model.Manga

interface MangaListView : MvpView {

	@StateStrategyType(AddToEndSingleTagStrategy::class, tag = "content")
	fun onListChanged(list: List<Manga>)

	@StateStrategyType(AddToEndStrategy::class, tag = "content")
	fun onListAppended(list: List<Manga>)

	@StateStrategyType(AddToEndSingleStrategy::class)
	fun onLoadingChanged(isLoading: Boolean)
}