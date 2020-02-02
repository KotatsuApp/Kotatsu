package org.koitharu.kotatsu.ui.main.list

import moxy.MvpView
import moxy.viewstate.strategy.*
import org.koitharu.kotatsu.core.model.MangaInfo

interface MangaListView<E> : MvpView {

	@StateStrategyType(AddToEndSingleTagStrategy::class, tag = "content")
	fun onListChanged(list: List<MangaInfo<E>>)

	@StateStrategyType(AddToEndStrategy::class, tag = "content")
	fun onListAppended(list: List<MangaInfo<E>>)

	@StateStrategyType(AddToEndSingleStrategy::class)
	fun onLoadingChanged(isLoading: Boolean)

	@StateStrategyType(OneExecutionStateStrategy::class)
	fun onError(e: Exception)
}