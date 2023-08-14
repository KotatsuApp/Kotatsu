package org.koitharu.kotatsu.scrobbling.common.ui.config.adapter

import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import org.koitharu.kotatsu.core.ui.BaseListAdapter
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.list.ui.adapter.emptyStateListAD
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblingInfo

class ScrobblingMangaAdapter(
	clickListener: OnListItemClickListener<ScrobblingInfo>,
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
) : BaseListAdapter<ListModel>() {

	init {
		delegatesManager.addDelegate(scrobblingMangaAD(clickListener, coil, lifecycleOwner))
			.addDelegate(scrobblingHeaderAD())
			.addDelegate(emptyStateListAD(coil, lifecycleOwner, null))
	}
}
