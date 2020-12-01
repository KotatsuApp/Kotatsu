package org.koitharu.kotatsu.list.ui.adapter

import coil.ImageLoader
import coil.request.Disposable
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.databinding.ItemMangaGridBinding
import org.koitharu.kotatsu.list.ui.model.MangaGridModel
import org.koitharu.kotatsu.utils.ext.enqueueWith
import org.koitharu.kotatsu.utils.ext.newImageRequest

fun mangaGridItemAD(
	coil: ImageLoader,
	clickListener: OnListItemClickListener<Manga>
) = adapterDelegateViewBinding<MangaGridModel, Any, ItemMangaGridBinding>(
	{ inflater, parent -> ItemMangaGridBinding.inflate(inflater, parent, false) }
) {

	var imageRequest: Disposable? = null

	itemView.setOnClickListener {
		clickListener.onItemClick(item.manga, it)
	}
	itemView.setOnLongClickListener {
		clickListener.onItemLongClick(item.manga, it)
	}

	bind {
		binding.textViewTitle.text = item.title
		imageRequest?.dispose()
		imageRequest = binding.imageViewCover.newImageRequest(item.coverUrl)
			.placeholder(R.drawable.ic_placeholder)
			.fallback(R.drawable.ic_placeholder)
			.error(R.drawable.ic_placeholder)
			.enqueueWith(coil)
	}

	onViewRecycled {
		imageRequest?.dispose()
		binding.imageViewCover.setImageDrawable(null)
	}
}