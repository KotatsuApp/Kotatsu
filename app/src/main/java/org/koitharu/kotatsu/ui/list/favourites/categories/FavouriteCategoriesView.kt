package org.koitharu.kotatsu.ui.list.favourites.categories

import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.StateStrategyType
import moxy.viewstate.strategy.alias.AddToEndSingle
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.ui.common.BaseMvpView

interface FavouriteCategoriesView : BaseMvpView {

	@StateStrategyType(AddToEndSingleStrategy::class)
	fun onCategoriesChanged(categories: List<FavouriteCategory>)

	@StateStrategyType(AddToEndSingleStrategy::class)
	fun onCheckedCategoriesChanged(checkedIds: Set<Int>)

	@AddToEndSingle
	override fun onLoadingStateChanged(isLoading: Boolean) = Unit
}