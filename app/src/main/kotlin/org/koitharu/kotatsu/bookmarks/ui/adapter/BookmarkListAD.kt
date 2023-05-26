package org.koitharu.kotatsu.bookmarks.ui.adapter

import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.bookmarks.domain.Bookmark
import org.koitharu.kotatsu.core.ui.image.CoverSizeResolver
import org.koitharu.kotatsu.core.ui.list.AdapterDelegateClickListenerAdapter
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.util.ext.decodeRegion
import org.koitharu.kotatsu.core.util.ext.disposeImageRequest
import org.koitharu.kotatsu.core.util.ext.enqueueWith
import org.koitharu.kotatsu.core.util.ext.newImageRequest
import org.koitharu.kotatsu.core.util.ext.source
import org.koitharu.kotatsu.databinding.ItemBookmarkBinding

fun bookmarkListAD(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	clickListener: OnListItemClickListener<Bookmark>,
) = adapterDelegateViewBinding<Bookmark, Bookmark, ItemBookmarkBinding>(
	{ inflater, parent -> ItemBookmarkBinding.inflate(inflater, parent, false) },
) {
	val listener = AdapterDelegateClickListenerAdapter(this, clickListener)

	binding.root.setOnClickListener(listener)
	binding.root.setOnLongClickListener(listener)

	bind {
		binding.imageViewThumb.newImageRequest(lifecycleOwner, item.imageUrl)?.run {
			size(CoverSizeResolver(binding.imageViewThumb))
			placeholder(R.drawable.ic_placeholder)
			fallback(R.drawable.ic_placeholder)
			error(R.drawable.ic_error_placeholder)
			allowRgb565(true)
			decodeRegion(item.scroll)
			source(item.manga.source)
			enqueueWith(coil)
		}
	}

	onViewRecycled {
		binding.imageViewThumb.disposeImageRequest()
	}
}
