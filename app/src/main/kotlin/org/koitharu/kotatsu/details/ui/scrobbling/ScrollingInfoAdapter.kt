package org.koitharu.kotatsu.details.ui.scrobbling

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import coil3.ImageLoader
import org.koitharu.kotatsu.core.ui.BaseListAdapter
import org.koitharu.kotatsu.list.ui.model.ListModel

class ScrollingInfoAdapter(
	lifecycleOwner: LifecycleOwner,
	coil: ImageLoader,
	fragmentManager: FragmentManager,
) : BaseListAdapter<ListModel>() {

	init {
		delegatesManager.addDelegate(scrobblingInfoAD(lifecycleOwner, coil, fragmentManager))
	}
}
