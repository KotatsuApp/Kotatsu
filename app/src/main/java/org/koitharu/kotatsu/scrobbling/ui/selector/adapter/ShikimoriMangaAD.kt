package org.koitharu.kotatsu.scrobbling.ui.selector.adapter

import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import coil.request.Disposable
import coil.util.CoilUtils
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.databinding.ItemMangaListBinding
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.scrobbling.domain.model.ScrobblerManga
import org.koitharu.kotatsu.utils.ext.enqueueWith
import org.koitharu.kotatsu.utils.ext.isLowRamDevice
import org.koitharu.kotatsu.utils.ext.newImageRequest
import org.koitharu.kotatsu.utils.ext.textAndVisible

fun shikimoriMangaAD(
	lifecycleOwner: LifecycleOwner,
	coil: ImageLoader,
	clickListener: OnListItemClickListener<ScrobblerManga>,
) = adapterDelegateViewBinding<ScrobblerManga, ListModel, ItemMangaListBinding>(
	{ inflater, parent -> ItemMangaListBinding.inflate(inflater, parent, false) }
) {

	var imageRequest: Disposable? = null

	itemView.setOnClickListener {
		clickListener.onItemClick(item, it)
	}

	bind {
		imageRequest?.dispose()
		binding.textViewTitle.text = item.name
		binding.textViewSubtitle.textAndVisible = item.altName
		imageRequest = binding.imageViewCover.newImageRequest(item.cover)
			.placeholder(R.drawable.ic_placeholder)
			.fallback(R.drawable.ic_placeholder)
			.error(R.drawable.ic_placeholder)
			.allowRgb565(isLowRamDevice(context))
			.lifecycle(lifecycleOwner)
			.enqueueWith(coil)
	}

	onViewRecycled {
		imageRequest?.dispose()
		imageRequest = null
		CoilUtils.dispose(binding.imageViewCover)
		binding.imageViewCover.setImageDrawable(null)
	}
}