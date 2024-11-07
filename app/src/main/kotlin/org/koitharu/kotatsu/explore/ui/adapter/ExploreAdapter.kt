package org.koitharu.kotatsu.explore.ui.adapter

import androidx.lifecycle.LifecycleOwner
import coil3.ImageLoader
import org.koitharu.kotatsu.core.ui.BaseListAdapter
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.explore.ui.model.MangaSourceItem
import org.koitharu.kotatsu.list.ui.adapter.ListItemType
import org.koitharu.kotatsu.list.ui.adapter.emptyHintAD
import org.koitharu.kotatsu.list.ui.adapter.listHeaderAD
import org.koitharu.kotatsu.list.ui.adapter.loadingStateAD
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.parsers.model.Manga

class ExploreAdapter(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	listener: ExploreListEventListener,
	clickListener: OnListItemClickListener<MangaSourceItem>,
	mangaClickListener: OnListItemClickListener<Manga>,
) : BaseListAdapter<ListModel>() {

	init {
		addDelegate(ListItemType.EXPLORE_BUTTONS, exploreButtonsAD(listener))
		addDelegate(
			ListItemType.EXPLORE_SUGGESTION,
			exploreRecommendationItemAD(coil, mangaClickListener, lifecycleOwner),
		)
		addDelegate(ListItemType.HEADER, listHeaderAD(listener))
		addDelegate(ListItemType.EXPLORE_SOURCE_LIST, exploreSourceListItemAD(coil, clickListener, lifecycleOwner))
		addDelegate(ListItemType.EXPLORE_SOURCE_GRID, exploreSourceGridItemAD(coil, clickListener, lifecycleOwner))
		addDelegate(ListItemType.HINT_EMPTY, emptyHintAD(coil, lifecycleOwner, listener))
		addDelegate(ListItemType.STATE_LOADING, loadingStateAD())
	}
}
