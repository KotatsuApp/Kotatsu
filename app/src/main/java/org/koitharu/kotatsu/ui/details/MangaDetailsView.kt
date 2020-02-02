package org.koitharu.kotatsu.ui.details

import moxy.MvpView
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.OneExecutionStateStrategy
import moxy.viewstate.strategy.StateStrategyType
import org.koitharu.kotatsu.core.model.MangaHistory
import org.koitharu.kotatsu.core.model.MangaInfo

interface MangaDetailsView : MvpView {

	@StateStrategyType(AddToEndSingleStrategy::class)
	fun onMangaUpdated(data: MangaInfo<MangaHistory?>)

	@StateStrategyType(AddToEndSingleStrategy::class)
	fun onLoadingStateChanged(isLoading: Boolean)

	@StateStrategyType(OneExecutionStateStrategy::class)
	fun onError(e: Exception)
}