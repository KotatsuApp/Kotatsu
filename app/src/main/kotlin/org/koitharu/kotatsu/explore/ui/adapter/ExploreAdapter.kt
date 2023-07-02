package org.koitharu.kotatsu.explore.ui.adapter

import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.explore.ui.model.ExploreItem

class ExploreAdapter(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	listener: ExploreListEventListener,
	clickListener: OnListItemClickListener<ExploreItem.Source>,
) : AsyncListDifferDelegationAdapter<ExploreItem>(ExploreDiffCallback()) {

	init {
		delegatesManager
			.addDelegate(ITEM_TYPE_BUTTONS, exploreButtonsAD(listener))
			.addDelegate(ITEM_TYPE_RECOMMENDATION_HEADER, exploreRecommendationHeaderAD())
			.addDelegate(ITEM_TYPE_RECOMMENDATION, exploreRecommendationItemAD(coil, listener, lifecycleOwner))
			.addDelegate(ITEM_TYPE_HEADER, exploreSourcesHeaderAD(listener))
			.addDelegate(ITEM_TYPE_SOURCE_LIST, exploreSourceListItemAD(coil, clickListener, lifecycleOwner))
			.addDelegate(ITEM_TYPE_SOURCE_GRID, exploreSourceGridItemAD(coil, clickListener, lifecycleOwner))
			.addDelegate(ITEM_TYPE_HINT, exploreEmptyHintListAD(listener))
			.addDelegate(ITEM_TYPE_LOADING, exploreLoadingAD())
	}

	companion object {

		const val ITEM_TYPE_BUTTONS = 0
		const val ITEM_TYPE_HEADER = 1
		const val ITEM_TYPE_SOURCE_LIST = 2
		const val ITEM_TYPE_SOURCE_GRID = 3
		const val ITEM_TYPE_HINT = 4
		const val ITEM_TYPE_LOADING = 5
		const val ITEM_TYPE_RECOMMENDATION_HEADER = 6
		const val ITEM_TYPE_RECOMMENDATION = 7
	}
}
