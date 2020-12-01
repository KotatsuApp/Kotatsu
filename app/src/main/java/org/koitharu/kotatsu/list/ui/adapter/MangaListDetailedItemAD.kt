package org.koitharu.kotatsu.list.ui.adapter

import coil.ImageLoader
import coil.request.Disposable
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.databinding.ItemMangaListDetailsBinding
import org.koitharu.kotatsu.list.ui.model.MangaListDetailedModel
import org.koitharu.kotatsu.utils.ext.enqueueWith
import org.koitharu.kotatsu.utils.ext.newImageRequest
import org.koitharu.kotatsu.utils.ext.textAndVisible

fun mangaListDetailedItemAD(
	coil: ImageLoader,
	clickListener: OnListItemClickListener<Manga>
) = adapterDelegateViewBinding<MangaListDetailedModel, Any, ItemMangaListDetailsBinding>(
	{ inflater, parent -> ItemMangaListDetailsBinding.inflate(inflater, parent, false) }
) {

	var imageRequest: Disposable? = null

	itemView.setOnClickListener {
		clickListener.onItemClick(item.manga, it)
	}
	itemView.setOnLongClickListener {
		clickListener.onItemLongClick(item.manga, it)
	}

	bind {
		imageRequest?.dispose()
		binding.textViewTitle.text = item.title
		binding.textViewSubtitle.textAndVisible = item.subtitle
		imageRequest = binding.imageViewCover.newImageRequest(item.coverUrl)
			.placeholder(R.drawable.ic_placeholder)
			.fallback(R.drawable.ic_placeholder)
			.error(R.drawable.ic_placeholder)
			.enqueueWith(coil)
		binding.textViewRating.textAndVisible = item.rating
		binding.textViewTags.text = item.tags
	}

	onViewRecycled {
		imageRequest?.dispose()
		binding.imageViewCover.setImageDrawable(null)
	}
}