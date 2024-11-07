package org.koitharu.kotatsu.tracker.ui.feed.adapter

import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import coil3.ImageLoader
import coil3.request.allowRgb565
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.util.ext.defaultPlaceholders
import org.koitharu.kotatsu.core.util.ext.drawableStart
import org.koitharu.kotatsu.core.util.ext.enqueueWith
import org.koitharu.kotatsu.core.util.ext.mangaSourceExtra
import org.koitharu.kotatsu.core.util.ext.newImageRequest
import org.koitharu.kotatsu.databinding.ItemFeedBinding
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.tracker.ui.feed.model.FeedItem

fun feedItemAD(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	clickListener: OnListItemClickListener<FeedItem>,
) = adapterDelegateViewBinding<FeedItem, ListModel, ItemFeedBinding>(
	{ inflater, parent -> ItemFeedBinding.inflate(inflater, parent, false) },
) {
	val indicatorNew = ContextCompat.getDrawable(context, R.drawable.ic_new)

	itemView.setOnClickListener {
		clickListener.onItemClick(item, it)
	}

	bind {
		binding.imageViewCover.newImageRequest(lifecycleOwner, item.imageUrl)?.run {
			defaultPlaceholders(context)
			allowRgb565(true)
			mangaSourceExtra(item.manga.source)
			enqueueWith(coil)
		}
		binding.textViewTitle.text = item.title
		binding.textViewSummary.text = context.resources.getQuantityString(
			R.plurals.new_chapters,
			item.count,
			item.count,
		)
		binding.textViewSummary.drawableStart = if (item.isNew) {
			indicatorNew
		} else {
			null
		}
	}
}
