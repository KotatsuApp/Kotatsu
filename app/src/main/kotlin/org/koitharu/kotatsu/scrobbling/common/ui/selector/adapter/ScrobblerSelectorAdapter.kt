package org.koitharu.kotatsu.scrobbling.common.ui.selector.adapter

import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.list.ui.adapter.ListStateHolderListener
import org.koitharu.kotatsu.list.ui.adapter.loadingFooterAD
import org.koitharu.kotatsu.list.ui.adapter.loadingStateAD
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.model.LoadingFooter
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblerManga
import org.koitharu.kotatsu.scrobbling.common.ui.selector.model.ScrobblerHint
import kotlin.jvm.internal.Intrinsics

class ScrobblerSelectorAdapter(
	lifecycleOwner: LifecycleOwner,
	coil: ImageLoader,
	clickListener: OnListItemClickListener<ScrobblerManga>,
	stateHolderListener: ListStateHolderListener,
) : AsyncListDifferDelegationAdapter<ListModel>(DiffCallback()) {

	init {
		delegatesManager.addDelegate(loadingStateAD())
			.addDelegate(scrobblingMangaAD(lifecycleOwner, coil, clickListener))
			.addDelegate(loadingFooterAD())
			.addDelegate(scrobblerHintAD(stateHolderListener))
	}

	private class DiffCallback : DiffUtil.ItemCallback<ListModel>() {

		override fun areItemsTheSame(oldItem: ListModel, newItem: ListModel): Boolean {
			return when {
				oldItem === newItem -> true
				oldItem is ScrobblerManga && newItem is ScrobblerManga -> oldItem.id == newItem.id
				oldItem is ScrobblerHint && newItem is ScrobblerHint -> oldItem.textPrimary == newItem.textPrimary
				oldItem is LoadingFooter && newItem is LoadingFooter -> oldItem.key == newItem.key
				else -> false
			}
		}

		override fun areContentsTheSame(oldItem: ListModel, newItem: ListModel): Boolean {
			return Intrinsics.areEqual(oldItem, newItem)
		}
	}
}
