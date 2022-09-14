package org.koitharu.kotatsu.search.ui.multi.adapter

import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import kotlin.jvm.internal.Intrinsics
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.list.ui.ItemSizeResolver
import org.koitharu.kotatsu.list.ui.MangaSelectionDecoration
import org.koitharu.kotatsu.list.ui.adapter.*
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.search.ui.multi.MultiSearchListModel

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
			.addDelegate(emptyStateListAD(coil, listener))
			.addDelegate(errorStateListAD(listener))
	}

	private class DiffCallback : DiffUtil.ItemCallback<ListModel>() {

		override fun areItemsTheSame(oldItem: ListModel, newItem: ListModel): Boolean {
			return when {
				oldItem is MultiSearchListModel && newItem is MultiSearchListModel -> {
					oldItem.source == newItem.source
				}
				else -> oldItem.javaClass == newItem.javaClass
			}
		}

		override fun areContentsTheSame(oldItem: ListModel, newItem: ListModel): Boolean {
			return Intrinsics.areEqual(oldItem, newItem)
		}
	}
}