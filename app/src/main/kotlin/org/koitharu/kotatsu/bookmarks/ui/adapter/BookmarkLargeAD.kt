package org.koitharu.kotatsu.bookmarks.ui.adapter

import androidx.lifecycle.LifecycleOwner
import coil3.ImageLoader
import coil3.request.allowRgb565
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.bookmarks.domain.Bookmark
import org.koitharu.kotatsu.core.ui.image.CoverSizeResolver
import org.koitharu.kotatsu.core.ui.list.AdapterDelegateClickListenerAdapter
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.util.ext.bookmarkExtra
import org.koitharu.kotatsu.core.util.ext.decodeRegion
import org.koitharu.kotatsu.core.util.ext.defaultPlaceholders
import org.koitharu.kotatsu.core.util.ext.enqueueWith
import org.koitharu.kotatsu.core.util.ext.newImageRequest
import org.koitharu.kotatsu.databinding.ItemBookmarkLargeBinding
import org.koitharu.kotatsu.list.ui.model.ListModel

fun bookmarkLargeAD(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	clickListener: OnListItemClickListener<Bookmark>,
) = adapterDelegateViewBinding<Bookmark, ListModel, ItemBookmarkLargeBinding>(
	{ inflater, parent -> ItemBookmarkLargeBinding.inflate(inflater, parent, false) },
) {
	AdapterDelegateClickListenerAdapter(this, clickListener).attach(itemView)

	bind {
		binding.imageViewThumb.newImageRequest(lifecycleOwner, item.imageLoadData)?.run {
			size(CoverSizeResolver(binding.imageViewThumb))
			defaultPlaceholders(context)
			allowRgb565(true)
			bookmarkExtra(item)
			decodeRegion(item.scroll)
			enqueueWith(coil)
		}
		binding.progressView.setProgress(item.percent, false)
	}
}
