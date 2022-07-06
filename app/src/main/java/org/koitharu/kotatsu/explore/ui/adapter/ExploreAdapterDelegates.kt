package org.koitharu.kotatsu.explore.ui.adapter

import android.view.View
import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import coil.request.Disposable
import coil.request.ImageRequest
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.databinding.ItemExploreButtonsBinding
import org.koitharu.kotatsu.databinding.ItemExploreHeaderBinding
import org.koitharu.kotatsu.databinding.ItemExploreSourceBinding
import org.koitharu.kotatsu.explore.ui.model.ExploreItem
import org.koitharu.kotatsu.utils.ext.enqueueWith

fun exploreButtonsDelegate() = adapterDelegateViewBinding<ExploreItem.Buttons, ExploreItem, ItemExploreButtonsBinding>(
	{ layoutInflater, parent -> ItemExploreButtonsBinding.inflate(layoutInflater, parent, false) }
) {

	binding.localStorage.requestFocus() // stub

} // TODO

fun sourceHeaderDelegate(
	listener: SourcesHeaderEventListener,
) = adapterDelegateViewBinding<ExploreItem.Header, ExploreItem, ItemExploreHeaderBinding>(
	{ layoutInflater, parent -> ItemExploreHeaderBinding.inflate(layoutInflater, parent, false) }
) {

	val listenerAdapter = View.OnClickListener {
		listener.onManageClick(itemView)
	}

	binding.buttonMore.setOnClickListener(listenerAdapter)

	bind {
		binding.textViewTitle.setText(R.string.remote_sources)
	}
}

fun sourceItemDelegate(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
) = adapterDelegateViewBinding<ExploreItem.Source, ExploreItem, ItemExploreSourceBinding>(
	{ layoutInflater, parent -> ItemExploreSourceBinding.inflate(layoutInflater, parent, false) },
	on = { item, _, _ -> item is ExploreItem.Source }
) {

	var imageRequest: Disposable? = null

	bind {
		binding.textViewTitle.text = item.source.title
		imageRequest = ImageRequest.Builder(context)
			.data(item.faviconUrl)
			.error(R.drawable.ic_favicon_fallback)
			.target(binding.imageViewCover)
			.lifecycle(lifecycleOwner)
			.enqueueWith(coil)
	}

	onViewRecycled {
		imageRequest?.dispose()
		imageRequest = null
	}
}