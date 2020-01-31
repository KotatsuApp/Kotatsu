package org.koitharu.kotatsu.ui.main.list

import moxy.MvpView
import moxy.viewstate.strategy.*
import org.koitharu.kotatsu.core.model.Manga

interface MangaListView : MvpView {

	@StateStrategyType(AddToEndSingleTagStrategy::class, tag = "content")
	fun onListChanged(list: List<Manga>)

	@StateStrategyType(AddToEndStrategy::class, tag = "content")
	fun onListAppended(list: List<Manga>)

	@StateStrategyType(AddToEndSingleStrategy::class)
	fun onLoadingChanged(isLoading: Boolean)

	@StateStrategyType(OneExecutionStateStrategy::class)
	fun onError(e: Exception)
}