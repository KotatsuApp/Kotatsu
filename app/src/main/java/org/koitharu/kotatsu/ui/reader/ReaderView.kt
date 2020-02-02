package org.koitharu.kotatsu.ui.reader

import moxy.MvpView
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.OneExecutionStateStrategy
import moxy.viewstate.strategy.StateStrategyType
import org.koitharu.kotatsu.core.model.MangaPage

interface ReaderView : MvpView {

	@StateStrategyType(AddToEndSingleStrategy::class)
	fun onPagesReady(pages: List<MangaPage>)

	@StateStrategyType(AddToEndSingleStrategy::class)
	fun onLoadingStateChanged(isLoading: Boolean)

	@StateStrategyType(OneExecutionStateStrategy::class)
	fun onError(e: Exception)
}