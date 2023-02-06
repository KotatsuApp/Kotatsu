package org.koitharu.kotatsu.list.ui.adapter

import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import com.google.android.material.badge.BadgeDrawable
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.databinding.ItemMangaGridBinding
import org.koitharu.kotatsu.history.domain.PROGRESS_NONE
import org.koitharu.kotatsu.list.ui.ItemSizeResolver
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.model.MangaGridModel
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.utils.ext.disposeImageRequest
import org.koitharu.kotatsu.utils.ext.enqueueWith
import org.koitharu.kotatsu.utils.ext.newImageRequest
import org.koitharu.kotatsu.utils.ext.referer
import org.koitharu.kotatsu.utils.image.CoverSizeResolver

fun mangaGridItemAD(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	clickListener: OnListItemClickListener<Manga>,
	sizeResolver: ItemSizeResolver?,
) = adapterDelegateViewBinding<MangaGridModel, ListModel, ItemMangaGridBinding>(
	{ inflater, parent -> ItemMangaGridBinding.inflate(inflater, parent, false) },
) {
	var badge: BadgeDrawable? = null

	itemView.setOnClickListener {
		clickListener.onItemClick(item.manga, it)
	}
	itemView.setOnLongClickListener {
		clickListener.onItemLongClick(item.manga, it)
	}
	sizeResolver?.attachToView(lifecycleOwner, itemView, binding.textViewTitle)

	bind { payloads ->
		binding.textViewTitle.text = item.title
		binding.progressView.setPercent(item.progress, MangaListAdapter.PAYLOAD_PROGRESS in payloads)
		binding.imageViewCover.newImageRequest(item.coverUrl, item.source)?.run {
			referer(item.manga.publicUrl)
			size(CoverSizeResolver(binding.imageViewCover))
			placeholder(R.drawable.ic_placeholder)
			fallback(R.drawable.ic_placeholder)
			error(R.drawable.ic_error_placeholder)
			allowRgb565(true)
			lifecycle(lifecycleOwner)
			enqueueWith(coil)
		}
		badge = itemView.bindBadge(badge, item.counter)
	}

	onViewRecycled {
		itemView.clearBadge(badge)
		binding.progressView.percent = PROGRESS_NONE
		badge = null
		binding.imageViewCover.disposeImageRequest()
	}
}
