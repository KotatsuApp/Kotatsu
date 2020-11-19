package org.koitharu.kotatsu.list.ui.adapter

import androidx.recyclerview.widget.DiffUtil
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.list.ui.model.IndeterminateProgress
import org.koitharu.kotatsu.list.ui.model.MangaGridModel
import org.koitharu.kotatsu.list.ui.model.MangaListDetailedModel
import org.koitharu.kotatsu.list.ui.model.MangaListModel
import kotlin.jvm.internal.Intrinsics

class MangaListAdapter(
	coil: ImageLoader,
	clickListener: OnListItemClickListener<Manga>
) : AsyncListDifferDelegationAdapter<Any>(DiffCallback) {

	init {
		delegatesManager.addDelegate(mangaListItemAD(coil, clickListener))
			.addDelegate(mangaListDetailedItemAD(coil, clickListener))
			.addDelegate(mangaGridItemAD(coil, clickListener))
			.addDelegate(indeterminateProgressAD())
	}

	private companion object DiffCallback : DiffUtil.ItemCallback<Any>() {

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
			else -> false
		}

		override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
			return Intrinsics.areEqual(oldItem, newItem)
		}

	}
}