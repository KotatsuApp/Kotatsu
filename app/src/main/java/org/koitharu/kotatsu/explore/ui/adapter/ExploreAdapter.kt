package org.koitharu.kotatsu.explore.ui.adapter

import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.explore.ui.model.ExploreItem

class ExploreAdapter(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	listener: ExploreListEventListener,
	clickListener: OnListItemClickListener<ExploreItem.Source>,
) : AsyncListDifferDelegationAdapter<ExploreItem>(
	ExploreDiffCallback(),
	exploreButtonsAD(listener),
	exploreSourcesHeaderAD(listener),
	exploreSourceItemAD(coil, clickListener, lifecycleOwner),
)