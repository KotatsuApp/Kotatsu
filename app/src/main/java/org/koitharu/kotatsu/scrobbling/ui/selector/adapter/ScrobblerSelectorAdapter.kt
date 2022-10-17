package org.koitharu.kotatsu.scrobbling.ui.selector.adapter

import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import kotlin.jvm.internal.Intrinsics
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.list.ui.adapter.loadingFooterAD
import org.koitharu.kotatsu.list.ui.adapter.loadingStateAD
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.scrobbling.domain.model.ScrobblerManga

class ScrobblerSelectorAdapter(
	lifecycleOwner: LifecycleOwner,
	coil: ImageLoader,
	clickListener: OnListItemClickListener<ScrobblerManga>,
) : AsyncListDifferDelegationAdapter<ListModel>(DiffCallback()) {

	init {
		delegatesManager.addDelegate(loadingStateAD())
			.addDelegate(scrobblerMangaAD(lifecycleOwner, coil, clickListener))
			.addDelegate(loadingFooterAD())
	}

	private class DiffCallback : DiffUtil.ItemCallback<ListModel>() {

		override fun areItemsTheSame(oldItem: ListModel, newItem: ListModel): Boolean {
			return when {
				oldItem === newItem -> true
				oldItem is ScrobblerManga && newItem is ScrobblerManga -> oldItem.id == newItem.id
				else -> false
			}
		}

		override fun areContentsTheSame(oldItem: ListModel, newItem: ListModel): Boolean {
			return Intrinsics.areEqual(oldItem, newItem)
		}
	}
}
