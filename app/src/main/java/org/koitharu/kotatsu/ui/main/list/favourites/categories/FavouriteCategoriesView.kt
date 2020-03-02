package org.koitharu.kotatsu.ui.main.list.favourites.categories

import moxy.MvpView
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.OneExecutionStateStrategy
import moxy.viewstate.strategy.StateStrategyType
import org.koitharu.kotatsu.core.model.FavouriteCategory

interface FavouriteCategoriesView : MvpView {

	@StateStrategyType(AddToEndSingleStrategy::class)
	fun onCategoriesChanged(categories: List<FavouriteCategory>)

	@StateStrategyType(AddToEndSingleStrategy::class)
	fun onCheckedCategoriesChanged(checkedIds: Set<Int>)

	@StateStrategyType(OneExecutionStateStrategy::class)
	fun onError(e: Throwable)
}