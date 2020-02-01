package org.koitharu.kotatsu.ui.main.details

import moxy.MvpView
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.OneExecutionStateStrategy
import moxy.viewstate.strategy.StateStrategyType
import org.koitharu.kotatsu.core.model.Manga

interface MangaDetailsView : MvpView {

	@StateStrategyType(AddToEndSingleStrategy::class)
	fun onMangaUpdated(manga: Manga)

	@StateStrategyType(AddToEndSingleStrategy::class)
	fun onLoadingStateChanged(isLoading: Boolean)

	@StateStrategyType(OneExecutionStateStrategy::class)
	fun onError(e: Exception)
}