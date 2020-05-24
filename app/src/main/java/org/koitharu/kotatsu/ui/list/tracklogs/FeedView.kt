package org.koitharu.kotatsu.ui.list.tracklogs

import moxy.viewstate.strategy.AddToEndSingleTagStrategy
import moxy.viewstate.strategy.AddToEndStrategy
import moxy.viewstate.strategy.StateStrategyType
import org.koitharu.kotatsu.core.model.TrackingLogItem
import org.koitharu.kotatsu.ui.common.BaseMvpView

interface FeedView : BaseMvpView {

	@StateStrategyType(AddToEndSingleTagStrategy::class, tag = "content")
	fun onListChanged(list: List<TrackingLogItem>)

	@StateStrategyType(AddToEndStrategy::class, tag = "content")
	fun onListAppended(list: List<TrackingLogItem>)

	@StateStrategyType(AddToEndSingleTagStrategy::class, tag = "content")
	fun onListError(e: Throwable)
}