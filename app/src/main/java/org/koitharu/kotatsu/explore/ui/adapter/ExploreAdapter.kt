package org.koitharu.kotatsu.explore.ui.adapter

import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import org.koitharu.kotatsu.explore.ui.model.ExploreItem

class ExploreAdapter(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	listener: SourcesHeaderEventListener,
) : AsyncListDifferDelegationAdapter<ExploreItem>(
	ExploreDiffCallback(),
	exploreButtonsDelegate(),
	sourceHeaderDelegate(listener),
	sourceItemDelegate(coil, lifecycleOwner),
) {

	init {
		delegatesManager
			.addDelegate(exploreButtonsDelegate())
			.addDelegate(sourceHeaderDelegate(listener = listener))
			.addDelegate(sourceItemDelegate(coil, lifecycleOwner))
	}


}