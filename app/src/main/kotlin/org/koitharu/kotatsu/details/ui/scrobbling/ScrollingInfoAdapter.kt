package org.koitharu.kotatsu.details.ui.scrobbling

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblingInfo

class ScrollingInfoAdapter(
	lifecycleOwner: LifecycleOwner,
	coil: ImageLoader,
	fragmentManager: FragmentManager,
) : AsyncListDifferDelegationAdapter<ScrobblingInfo>(DiffCallback()) {

	init {
		delegatesManager.addDelegate(scrobblingInfoAD(lifecycleOwner, coil, fragmentManager))
	}

	private class DiffCallback : DiffUtil.ItemCallback<ScrobblingInfo>() {

		override fun areItemsTheSame(oldItem: ScrobblingInfo, newItem: ScrobblingInfo): Boolean {
			return oldItem.scrobbler == newItem.scrobbler
		}

		override fun areContentsTheSame(oldItem: ScrobblingInfo, newItem: ScrobblingInfo): Boolean {
			return oldItem == newItem
		}

		override fun getChangePayload(oldItem: ScrobblingInfo, newItem: ScrobblingInfo): Any {
			return Unit
		}
	}
}
