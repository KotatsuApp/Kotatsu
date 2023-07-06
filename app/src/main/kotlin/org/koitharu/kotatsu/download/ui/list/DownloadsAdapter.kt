package org.koitharu.kotatsu.download.ui.list

import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import org.koitharu.kotatsu.core.ui.BaseListAdapter
import org.koitharu.kotatsu.list.ui.adapter.emptyStateListAD
import org.koitharu.kotatsu.list.ui.adapter.listHeaderAD
import org.koitharu.kotatsu.list.ui.adapter.loadingStateAD
import org.koitharu.kotatsu.list.ui.model.ListModel

class DownloadsAdapter(
	lifecycleOwner: LifecycleOwner,
	coil: ImageLoader,
	listener: DownloadItemListener,
) : BaseListAdapter<ListModel>() {

	init {
		delegatesManager.addDelegate(ITEM_TYPE_DOWNLOAD, downloadItemAD(lifecycleOwner, coil, listener))
			.addDelegate(loadingStateAD())
			.addDelegate(emptyStateListAD(coil, lifecycleOwner, null))
			.addDelegate(listHeaderAD(null))
	}

	companion object {
		const val ITEM_TYPE_DOWNLOAD = 0
	}
}
