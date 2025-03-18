package org.koitharu.kotatsu.details.ui.scrobbling

import androidx.lifecycle.LifecycleOwner
import coil3.ImageLoader
import org.koitharu.kotatsu.core.nav.AppRouter
import org.koitharu.kotatsu.core.ui.BaseListAdapter
import org.koitharu.kotatsu.list.ui.model.ListModel

class ScrollingInfoAdapter(
	lifecycleOwner: LifecycleOwner,
	coil: ImageLoader,
	router: AppRouter,
) : BaseListAdapter<ListModel>() {

	init {
		delegatesManager.addDelegate(scrobblingInfoAD(lifecycleOwner, coil, router))
	}
}
