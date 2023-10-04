package org.koitharu.kotatsu.tracker.ui.feed.adapter

import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.util.ext.enqueueWith
import org.koitharu.kotatsu.core.util.ext.newImageRequest
import org.koitharu.kotatsu.core.util.ext.source
import org.koitharu.kotatsu.databinding.ItemFeedBinding
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.tracker.ui.feed.model.FeedItem

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
		val alpha = if (item.isNew) 1f else 0.5f
		binding.textViewTitle.alpha = alpha
		binding.textViewSummary.alpha = alpha
		binding.imageViewCover.newImageRequest(lifecycleOwner, item.imageUrl)?.run {
			placeholder(R.drawable.ic_placeholder)
			fallback(R.drawable.ic_placeholder)
			error(R.drawable.ic_error_placeholder)
			allowRgb565(true)
			source(item.manga.source)
			enqueueWith(coil)
		}
		binding.textViewTitle.text = item.title
		binding.textViewSummary.text = context.resources.getQuantityString(
			R.plurals.new_chapters,
			item.count,
			item.count,
		)
	}
}
