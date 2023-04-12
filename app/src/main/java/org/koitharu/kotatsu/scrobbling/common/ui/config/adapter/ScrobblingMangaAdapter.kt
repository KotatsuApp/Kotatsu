package org.koitharu.kotatsu.scrobbling.common.ui.config.adapter

import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.list.ui.adapter.emptyStateListAD
import org.koitharu.kotatsu.list.ui.model.EmptyState
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblingInfo
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblingStatus

class ScrobblingMangaAdapter(
	clickListener: OnListItemClickListener<ScrobblingInfo>,
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
) : AsyncListDifferDelegationAdapter<ListModel>(DiffCallback()) {

	init {
		delegatesManager.addDelegate(scrobblingMangaAD(clickListener, coil, lifecycleOwner))
			.addDelegate(scrobblingHeaderAD())
			.addDelegate(emptyStateListAD(coil, lifecycleOwner, null))
	}

	private class DiffCallback : DiffUtil.ItemCallback<ListModel>() {

		override fun areItemsTheSame(oldItem: ListModel, newItem: ListModel): Boolean {
			return when {
				oldItem is ScrobblingInfo && newItem is ScrobblingInfo -> {
					oldItem.targetId == newItem.targetId && oldItem.mangaId == newItem.mangaId
				}

				oldItem is ScrobblingStatus && newItem is ScrobblingStatus -> {
					oldItem.ordinal == newItem.ordinal
				}

				oldItem is EmptyState && newItem is EmptyState -> true

				else -> false
			}
		}

		override fun areContentsTheSame(oldItem: ListModel, newItem: ListModel): Boolean {
			return oldItem == newItem
		}
	}
}
