package org.koitharu.kotatsu.bookmarks.ui.adapter

import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.list.AdapterDelegateClickListenerAdapter
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.bookmarks.domain.Bookmark
import org.koitharu.kotatsu.databinding.ItemBookmarkBinding
import org.koitharu.kotatsu.utils.ext.disposeImageRequest
import org.koitharu.kotatsu.utils.ext.enqueueWith
import org.koitharu.kotatsu.utils.ext.newImageRequest
import org.koitharu.kotatsu.utils.ext.source

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
			placeholder(R.drawable.ic_placeholder)
			fallback(R.drawable.ic_placeholder)
			error(R.drawable.ic_error_placeholder)
			allowRgb565(true)
			source(item.manga.source)
			enqueueWith(coil)
		}
	}

	onViewRecycled {
		binding.imageViewThumb.disposeImageRequest()
	}
}
