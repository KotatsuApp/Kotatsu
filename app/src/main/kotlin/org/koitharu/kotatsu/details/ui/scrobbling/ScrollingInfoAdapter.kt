package org.koitharu.kotatsu.details.ui.scrobbling

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import org.koitharu.kotatsu.list.ui.ListModelDiffCallback
import org.koitharu.kotatsu.list.ui.model.ListModel

class ScrollingInfoAdapter(
	lifecycleOwner: LifecycleOwner,
	coil: ImageLoader,
	fragmentManager: FragmentManager,
) : AsyncListDifferDelegationAdapter<ListModel>(ListModelDiffCallback) {

	init {
		delegatesManager.addDelegate(scrobblingInfoAD(lifecycleOwner, coil, fragmentManager))
	}
}
