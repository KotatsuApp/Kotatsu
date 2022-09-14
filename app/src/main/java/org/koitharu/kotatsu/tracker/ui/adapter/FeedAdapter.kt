package org.koitharu.kotatsu.tracker.ui.adapter

import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import kotlin.jvm.internal.Intrinsics
import org.koitharu.kotatsu.core.ui.DateTimeAgo
import org.koitharu.kotatsu.list.ui.adapter.*
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.tracker.ui.model.FeedItem

class FeedAdapter(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	listener: MangaListListener,
) : AsyncListDifferDelegationAdapter<ListModel>(DiffCallback()) {

	init {
		delegatesManager
			.addDelegate(ITEM_TYPE_FEED, feedItemAD(coil, lifecycleOwner, listener))
			.addDelegate(ITEM_TYPE_LOADING_FOOTER, loadingFooterAD())
			.addDelegate(ITEM_TYPE_LOADING_STATE, loadingStateAD())
			.addDelegate(ITEM_TYPE_ERROR_FOOTER, errorFooterAD(listener))
			.addDelegate(ITEM_TYPE_ERROR_STATE, errorStateListAD(listener))
			.addDelegate(ITEM_TYPE_EMPTY, emptyStateListAD(coil, listener))
			.addDelegate(ITEM_TYPE_HEADER, listHeaderAD())
			.addDelegate(ITEM_TYPE_DATE_HEADER, relatedDateItemAD())
	}

	private class DiffCallback : DiffUtil.ItemCallback<ListModel>() {

		override fun areItemsTheSame(oldItem: ListModel, newItem: ListModel) = when {
			oldItem is FeedItem && newItem is FeedItem -> {
				oldItem.id == newItem.id
			}
			oldItem is DateTimeAgo && newItem is DateTimeAgo -> {
				oldItem == newItem
			}
			else -> oldItem.javaClass == newItem.javaClass
		}

		override fun areContentsTheSame(oldItem: ListModel, newItem: ListModel): Boolean {
			return Intrinsics.areEqual(oldItem, newItem)
		}
	}

	companion object {

		const val ITEM_TYPE_FEED = 0
		const val ITEM_TYPE_LOADING_FOOTER = 1
		const val ITEM_TYPE_LOADING_STATE = 2
		const val ITEM_TYPE_ERROR_STATE = 3
		const val ITEM_TYPE_ERROR_FOOTER = 4
		const val ITEM_TYPE_EMPTY = 5
		const val ITEM_TYPE_HEADER = 6
		const val ITEM_TYPE_DATE_HEADER = 7
	}
}