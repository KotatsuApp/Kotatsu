package org.koitharu.kotatsu.search.ui.multi.adapter

import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import coil.ImageLoader
import org.koitharu.kotatsu.core.ui.BaseListAdapter
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.list.ui.MangaSelectionDecoration
import org.koitharu.kotatsu.list.ui.adapter.MangaListListener
import org.koitharu.kotatsu.list.ui.adapter.emptyStateListAD
import org.koitharu.kotatsu.list.ui.adapter.errorStateListAD
import org.koitharu.kotatsu.list.ui.adapter.loadingFooterAD
import org.koitharu.kotatsu.list.ui.adapter.loadingStateAD
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.size.ItemSizeResolver
import org.koitharu.kotatsu.search.ui.multi.MultiSearchListModel

class MultiSearchAdapter(
	lifecycleOwner: LifecycleOwner,
	coil: ImageLoader,
	listener: MangaListListener,
	itemClickListener: OnListItemClickListener<MultiSearchListModel>,
	sizeResolver: ItemSizeResolver,
	selectionDecoration: MangaSelectionDecoration,
) : BaseListAdapter<ListModel>() {

	init {
		val pool = RecycledViewPool()
		delegatesManager
			.addDelegate(
				searchResultsAD(
					sharedPool = pool,
					lifecycleOwner = lifecycleOwner,
					coil = coil,
					sizeResolver = sizeResolver,
					selectionDecoration = selectionDecoration,
					listener = listener,
					itemClickListener = itemClickListener,
				),
			)
			.addDelegate(loadingStateAD())
			.addDelegate(loadingFooterAD())
			.addDelegate(emptyStateListAD(coil, lifecycleOwner, listener))
			.addDelegate(errorStateListAD(listener))
	}
}
