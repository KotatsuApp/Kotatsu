package org.koitharu.kotatsu.explore.ui.adapter

import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import org.koitharu.kotatsu.core.ui.BaseListAdapter
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.ui.widgets.TipView
import org.koitharu.kotatsu.explore.ui.model.MangaSourceItem
import org.koitharu.kotatsu.list.ui.adapter.emptyHintAD
import org.koitharu.kotatsu.list.ui.adapter.listHeaderAD
import org.koitharu.kotatsu.list.ui.adapter.loadingStateAD
import org.koitharu.kotatsu.list.ui.adapter.tipAD
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.parsers.model.Manga

class ExploreAdapter(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	listener: ExploreListEventListener,
	tipClickListener: TipView.OnButtonClickListener,
	clickListener: OnListItemClickListener<MangaSourceItem>,
	mangaClickListener: OnListItemClickListener<Manga>,
) : BaseListAdapter<ListModel>() {

	init {
		delegatesManager
			.addDelegate(ITEM_TYPE_BUTTONS, exploreButtonsAD(listener))
			.addDelegate(
				ITEM_TYPE_RECOMMENDATION,
				exploreRecommendationItemAD(coil, listener, mangaClickListener, lifecycleOwner),
			)
			.addDelegate(ITEM_TYPE_HEADER, listHeaderAD(listener))
			.addDelegate(ITEM_TYPE_SOURCE_LIST, exploreSourceListItemAD(coil, clickListener, lifecycleOwner))
			.addDelegate(ITEM_TYPE_SOURCE_GRID, exploreSourceGridItemAD(coil, clickListener, lifecycleOwner))
			.addDelegate(ITEM_TYPE_HINT, emptyHintAD(coil, lifecycleOwner, listener))
			.addDelegate(ITEM_TYPE_LOADING, loadingStateAD())
			.addDelegate(ITEM_TIP, tipAD(tipClickListener))
	}

	companion object {

		const val ITEM_TYPE_BUTTONS = 0
		const val ITEM_TYPE_HEADER = 1
		const val ITEM_TYPE_SOURCE_LIST = 2
		const val ITEM_TYPE_SOURCE_GRID = 3
		const val ITEM_TYPE_HINT = 4
		const val ITEM_TYPE_LOADING = 5
		const val ITEM_TYPE_RECOMMENDATION = 6
		const val ITEM_TIP = 7
	}
}
