package org.koitharu.kotatsu.tracker.ui

import androidx.recyclerview.widget.DiffUtil
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.list.ui.adapter.indeterminateProgressAD
import org.koitharu.kotatsu.list.ui.model.IndeterminateProgress
import org.koitharu.kotatsu.tracker.ui.adapter.feedItemAD
import org.koitharu.kotatsu.tracker.ui.model.FeedItem
import kotlin.jvm.internal.Intrinsics

class FeedAdapter(
	coil: ImageLoader,
	clickListener: OnListItemClickListener<Manga>
) : AsyncListDifferDelegationAdapter<Any>(DiffCallback()) {

	init {
		delegatesManager.addDelegate(ITEM_TYPE_FEED, feedItemAD(coil, clickListener))
			.addDelegate(ITEM_TYPE_PROGRESS, indeterminateProgressAD())
	}

	private class DiffCallback : DiffUtil.ItemCallback<Any>() {

		override fun areItemsTheSame(oldItem: Any, newItem: Any) = when {
			oldItem is FeedItem && newItem is FeedItem -> {
				oldItem.id == newItem.id
			}
			oldItem == IndeterminateProgress && newItem == IndeterminateProgress -> {
				true
			}
			else -> false
		}

		override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
			return Intrinsics.areEqual(oldItem, newItem)
		}
	}

	companion object {

		const val ITEM_TYPE_FEED = 0
		const val ITEM_TYPE_PROGRESS = 1
	}
}