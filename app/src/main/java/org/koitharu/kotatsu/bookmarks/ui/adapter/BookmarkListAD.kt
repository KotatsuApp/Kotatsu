package org.koitharu.kotatsu.bookmarks.ui.adapter

import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import coil.request.Disposable
import coil.util.CoilUtils
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.list.AdapterDelegateClickListenerAdapter
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.bookmarks.domain.Bookmark
import org.koitharu.kotatsu.databinding.ItemBookmarkBinding
import org.koitharu.kotatsu.utils.ext.enqueueWith
import org.koitharu.kotatsu.utils.ext.isLowRamDevice
import org.koitharu.kotatsu.utils.ext.newImageRequest
import org.koitharu.kotatsu.utils.ext.referer

fun bookmarkListAD(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	clickListener: OnListItemClickListener<Bookmark>,
) = adapterDelegateViewBinding<Bookmark, Bookmark, ItemBookmarkBinding>(
	{ inflater, parent -> ItemBookmarkBinding.inflate(inflater, parent, false) }
) {

	var imageRequest: Disposable? = null
	val listener = AdapterDelegateClickListenerAdapter(this, clickListener)

	binding.root.setOnClickListener(listener)
	binding.root.setOnLongClickListener(listener)

	bind {
		imageRequest?.dispose()
		imageRequest = binding.imageViewThumb.newImageRequest(item.imageUrl)
			.referer(item.manga.publicUrl)
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
		CoilUtils.dispose(binding.imageViewThumb)
		binding.imageViewThumb.setImageDrawable(null)
	}
}