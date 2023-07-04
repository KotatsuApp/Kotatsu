package org.koitharu.kotatsu.search.ui.multi.adapter

import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.list.ui.MangaSelectionDecoration
import org.koitharu.kotatsu.list.ui.adapter.MangaListListener
import org.koitharu.kotatsu.list.ui.adapter.emptyStateListAD
import org.koitharu.kotatsu.list.ui.adapter.errorStateListAD
import org.koitharu.kotatsu.list.ui.adapter.loadingFooterAD
import org.koitharu.kotatsu.list.ui.adapter.loadingStateAD
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.model.LoadingFooter
import org.koitharu.kotatsu.list.ui.size.ItemSizeResolver
import org.koitharu.kotatsu.search.ui.multi.MultiSearchListModel
import kotlin.jvm.internal.Intrinsics

class MultiSearchAdapter(
	lifecycleOwner: LifecycleOwner,
	coil: ImageLoader,
	listener: MangaListListener,
	itemClickListener: OnListItemClickListener<MultiSearchListModel>,
	sizeResolver: ItemSizeResolver,
	selectionDecoration: MangaSelectionDecoration,
) : AsyncListDifferDelegationAdapter<ListModel>(DiffCallback()) {

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

	private class DiffCallback : DiffUtil.ItemCallback<ListModel>() {

		override fun areItemsTheSame(oldItem: ListModel, newItem: ListModel): Boolean {
			return when {
				oldItem is MultiSearchListModel && newItem is MultiSearchListModel -> {
					oldItem.source == newItem.source
				}

				oldItem is LoadingFooter && newItem is LoadingFooter -> {
					oldItem.key == newItem.key
				}

				else -> oldItem.javaClass == newItem.javaClass
			}
		}

		override fun areContentsTheSame(oldItem: ListModel, newItem: ListModel): Boolean {
			return Intrinsics.areEqual(oldItem, newItem)
		}
	}
}
