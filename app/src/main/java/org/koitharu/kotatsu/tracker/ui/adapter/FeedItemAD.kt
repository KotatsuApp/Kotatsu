package org.koitharu.kotatsu.tracker.ui.adapter

import coil.ImageLoader
import coil.request.Disposable
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateLayoutContainer
import kotlinx.android.synthetic.main.item_tracklog.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.tracker.ui.model.FeedItem
import org.koitharu.kotatsu.utils.ext.enqueueWith
import org.koitharu.kotatsu.utils.ext.newImageRequest

fun feedItemAD(
	coil: ImageLoader,
	clickListener: OnListItemClickListener<Manga>
) = adapterDelegateLayoutContainer<FeedItem, Any>(R.layout.item_tracklog) {

	var imageRequest: Disposable? = null

	itemView.setOnClickListener {
		clickListener.onItemClick(item.manga, it)
	}

	bind {
		imageRequest?.dispose()
		imageRequest = imageView_cover.newImageRequest(item.imageUrl)
			.placeholder(R.drawable.ic_placeholder)
			.fallback(R.drawable.ic_placeholder)
			.error(R.drawable.ic_placeholder)
			.enqueueWith(coil)
		textView_title.text = item.title
		textView_subtitle.text = item.subtitle
		textView_chapters.text = item.chapters
	}

	onViewRecycled {
		imageRequest?.dispose()
		imageView_cover.setImageDrawable(null)
	}
}