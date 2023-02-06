package org.koitharu.kotatsu.tracker.ui.feed.adapter

import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.databinding.ItemFeedBinding
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.tracker.ui.feed.model.FeedItem
import org.koitharu.kotatsu.utils.ext.disposeImageRequest
import org.koitharu.kotatsu.utils.ext.enqueueWith
import org.koitharu.kotatsu.utils.ext.isBold
import org.koitharu.kotatsu.utils.ext.newImageRequest

fun feedItemAD(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	clickListener: OnListItemClickListener<Manga>,
) = adapterDelegateViewBinding<FeedItem, ListModel, ItemFeedBinding>(
	{ inflater, parent -> ItemFeedBinding.inflate(inflater, parent, false) },
) {
	itemView.setOnClickListener {
		clickListener.onItemClick(item.manga, it)
	}

	bind {
		binding.textViewTitle.isBold = item.isNew
		binding.textViewSummary.isBold = item.isNew
		binding.imageViewCover.newImageRequest(item.imageUrl, item.manga.source)?.run {
			placeholder(R.drawable.ic_placeholder)
			fallback(R.drawable.ic_placeholder)
			error(R.drawable.ic_error_placeholder)
			allowRgb565(true)
			lifecycle(lifecycleOwner)
			enqueueWith(coil)
		}
		binding.textViewTitle.text = item.title
		binding.textViewSummary.text = context.resources.getQuantityString(
			R.plurals.new_chapters,
			item.count,
			item.count,
		)
	}

	onViewRecycled {
		binding.imageViewCover.disposeImageRequest()
	}
}
