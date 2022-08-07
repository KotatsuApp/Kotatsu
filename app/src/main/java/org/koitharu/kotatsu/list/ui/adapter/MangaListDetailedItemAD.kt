package org.koitharu.kotatsu.list.ui.adapter

import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import com.google.android.material.badge.BadgeDrawable
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.databinding.ItemMangaListDetailsBinding
import org.koitharu.kotatsu.history.domain.PROGRESS_NONE
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.model.MangaListDetailedModel
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.utils.ext.*

fun mangaListDetailedItemAD(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	clickListener: OnListItemClickListener<Manga>,
) = adapterDelegateViewBinding<MangaListDetailedModel, ListModel, ItemMangaListDetailsBinding>(
	{ inflater, parent -> ItemMangaListDetailsBinding.inflate(inflater, parent, false) },
) {
	var badge: BadgeDrawable? = null

	itemView.setOnClickListener {
		clickListener.onItemClick(item.manga, it)
	}
	itemView.setOnLongClickListener {
		clickListener.onItemLongClick(item.manga, it)
	}

	bind { payloads ->
		binding.textViewTitle.text = item.title
		binding.textViewSubtitle.textAndVisible = item.subtitle
		binding.progressView.setPercent(item.progress, MangaListAdapter.PAYLOAD_PROGRESS in payloads)
		binding.imageViewCover.newImageRequest(item.coverUrl)?.run {
			referer(item.manga.publicUrl)
			placeholder(R.drawable.ic_placeholder)
			fallback(R.drawable.ic_placeholder)
			error(R.drawable.ic_error_placeholder)
			allowRgb565(true)
			lifecycle(lifecycleOwner)
			enqueueWith(coil)
		}
		binding.textViewRating.textAndVisible = item.rating
		binding.textViewTags.text = item.tags
		itemView.bindBadge(badge, item.counter)
	}

	onViewRecycled {
		itemView.clearBadge(badge)
		binding.progressView.percent = PROGRESS_NONE
		badge = null
		binding.imageViewCover.disposeImageRequest()
	}
}
