package org.koitharu.kotatsu.scrobbling.common.ui.selector.adapter

import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import org.koitharu.kotatsu.core.ui.BaseListAdapter
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.list.ui.adapter.ListStateHolderListener
import org.koitharu.kotatsu.list.ui.adapter.loadingFooterAD
import org.koitharu.kotatsu.list.ui.adapter.loadingStateAD
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblerManga

class ScrobblerSelectorAdapter(
	lifecycleOwner: LifecycleOwner,
	coil: ImageLoader,
	clickListener: OnListItemClickListener<ScrobblerManga>,
	stateHolderListener: ListStateHolderListener,
) : BaseListAdapter<ListModel>() {

	init {
		delegatesManager.addDelegate(loadingStateAD())
			.addDelegate(scrobblingMangaAD(lifecycleOwner, coil, clickListener))
			.addDelegate(loadingFooterAD())
			.addDelegate(scrobblerHintAD(stateHolderListener))
	}
}
