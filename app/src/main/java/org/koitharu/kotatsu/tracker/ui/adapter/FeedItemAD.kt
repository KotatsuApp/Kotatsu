package org.koitharu.kotatsu.tracker.ui.adapter

import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import coil.request.Disposable
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.databinding.ItemFeedBinding
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.tracker.ui.model.FeedItem
import org.koitharu.kotatsu.utils.ext.enqueueWith
import org.koitharu.kotatsu.utils.ext.newImageRequest

fun feedItemAD(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	clickListener: OnListItemClickListener<Manga>
) = adapterDelegateViewBinding<FeedItem, ListModel, ItemFeedBinding>(
	{ inflater, parent -> ItemFeedBinding.inflate(inflater, parent, false) }
) {

	var imageRequest: Disposable? = null

	itemView.setOnClickListener {
		clickListener.onItemClick(item.manga, it)
	}

	bind {
		imageRequest?.dispose()
		imageRequest = binding.imageViewCover.newImageRequest(item.imageUrl)
			.placeholder(R.drawable.ic_placeholder)
			.fallback(R.drawable.ic_placeholder)
			.error(R.drawable.ic_placeholder)
			.allowRgb565(true)
			.lifecycle(lifecycleOwner)
			.enqueueWith(coil)
		binding.textViewTitle.text = item.title
		binding.textViewSummary.text = context.resources.getQuantityString(
			R.plurals.new_chapters,
			item.count,
			item.count,
		)
	}

	onViewRecycled {
		imageRequest?.dispose()
		binding.imageViewCover.setImageDrawable(null)
	}
}