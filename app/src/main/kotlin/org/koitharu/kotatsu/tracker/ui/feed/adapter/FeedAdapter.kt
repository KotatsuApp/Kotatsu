package org.koitharu.kotatsu.tracker.ui.feed.adapter

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import org.koitharu.kotatsu.core.ui.list.fastscroll.FastScroller
import org.koitharu.kotatsu.core.ui.model.DateTimeAgo
import org.koitharu.kotatsu.list.ui.adapter.MangaListListener
import org.koitharu.kotatsu.list.ui.adapter.emptyStateListAD
import org.koitharu.kotatsu.list.ui.adapter.errorFooterAD
import org.koitharu.kotatsu.list.ui.adapter.errorStateListAD
import org.koitharu.kotatsu.list.ui.adapter.loadingFooterAD
import org.koitharu.kotatsu.list.ui.adapter.loadingStateAD
import org.koitharu.kotatsu.list.ui.adapter.relatedDateItemAD
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.model.LoadingFooter
import org.koitharu.kotatsu.tracker.ui.feed.model.FeedItem
import kotlin.jvm.internal.Intrinsics

class FeedAdapter(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	listener: MangaListListener,
) : AsyncListDifferDelegationAdapter<ListModel>(DiffCallback()), FastScroller.SectionIndexer {

	init {
		delegatesManager
			.addDelegate(ITEM_TYPE_FEED, feedItemAD(coil, lifecycleOwner, listener))
			.addDelegate(ITEM_TYPE_LOADING_FOOTER, loadingFooterAD())
			.addDelegate(ITEM_TYPE_LOADING_STATE, loadingStateAD())
			.addDelegate(ITEM_TYPE_ERROR_FOOTER, errorFooterAD(listener))
			.addDelegate(ITEM_TYPE_ERROR_STATE, errorStateListAD(listener))
			.addDelegate(ITEM_TYPE_EMPTY, emptyStateListAD(coil, lifecycleOwner, listener))
			.addDelegate(ITEM_TYPE_DATE_HEADER, relatedDateItemAD())
	}

	override fun getSectionText(context: Context, position: Int): CharSequence? {
		val list = items
		for (i in (0..position).reversed()) {
			val item = list.getOrNull(i) ?: continue
			if (item is DateTimeAgo) {
				return item.format(context.resources)
			}
		}
		return null
	}

	private class DiffCallback : DiffUtil.ItemCallback<ListModel>() {

		override fun areItemsTheSame(oldItem: ListModel, newItem: ListModel) = when {
			oldItem is FeedItem && newItem is FeedItem -> {
				oldItem.id == newItem.id
			}

			oldItem is DateTimeAgo && newItem is DateTimeAgo -> {
				oldItem == newItem
			}

			oldItem is LoadingFooter && newItem is LoadingFooter -> {
				oldItem.key == newItem.key
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
