package org.koitharu.kotatsu.list.ui.adapter

import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.ui.DateTimeAgo
import org.koitharu.kotatsu.list.ui.model.IndeterminateProgress
import org.koitharu.kotatsu.list.ui.model.MangaGridModel
import org.koitharu.kotatsu.list.ui.model.MangaListDetailedModel
import org.koitharu.kotatsu.list.ui.model.MangaListModel
import kotlin.jvm.internal.Intrinsics

class MangaListAdapter(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	clickListener: OnListItemClickListener<Manga>
) : AsyncListDifferDelegationAdapter<Any>(DiffCallback()) {

	init {
		delegatesManager
			.addDelegate(
				ITEM_TYPE_MANGA_LIST,
				mangaListItemAD(coil, lifecycleOwner, clickListener)
			)
			.addDelegate(
				ITEM_TYPE_MANGA_LIST_DETAILED,
				mangaListDetailedItemAD(coil, lifecycleOwner, clickListener)
			)
			.addDelegate(ITEM_TYPE_MANGA_GRID, mangaGridItemAD(coil, lifecycleOwner, clickListener))
			.addDelegate(ITEM_TYPE_PROGRESS, indeterminateProgressAD())
			.addDelegate(ITEM_TYPE_DATE, relatedDateItemAD())
	}

	private class DiffCallback : DiffUtil.ItemCallback<Any>() {

		override fun areItemsTheSame(oldItem: Any, newItem: Any) = when {
			oldItem is MangaListModel && newItem is MangaListModel -> {
				oldItem.id == newItem.id
			}
			oldItem is MangaListDetailedModel && newItem is MangaListDetailedModel -> {
				oldItem.id == newItem.id
			}
			oldItem is MangaGridModel && newItem is MangaGridModel -> {
				oldItem.id == newItem.id
			}
			oldItem == IndeterminateProgress && newItem == IndeterminateProgress -> {
				true
			}
			oldItem is DateTimeAgo && newItem is DateTimeAgo -> {
				oldItem == newItem
			}
			else -> false
		}

		override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
			return Intrinsics.areEqual(oldItem, newItem)
		}
	}

	companion object {

		const val ITEM_TYPE_MANGA_LIST = 0
		const val ITEM_TYPE_MANGA_LIST_DETAILED = 1
		const val ITEM_TYPE_MANGA_GRID = 2
		const val ITEM_TYPE_PROGRESS = 3
		const val ITEM_TYPE_DATE = 4
	}
}