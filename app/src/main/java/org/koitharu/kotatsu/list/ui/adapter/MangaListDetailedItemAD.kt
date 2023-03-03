package org.koitharu.kotatsu.list.ui.adapter

import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.chip.Chip
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.widgets.ChipsView
import org.koitharu.kotatsu.databinding.ItemMangaListDetailsBinding
import org.koitharu.kotatsu.history.domain.PROGRESS_NONE
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.model.MangaListDetailedModel
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.utils.ext.disposeImageRequest
import org.koitharu.kotatsu.utils.ext.enqueueWith
import org.koitharu.kotatsu.utils.ext.newImageRequest
import org.koitharu.kotatsu.utils.ext.source
import org.koitharu.kotatsu.utils.ext.textAndVisible
import org.koitharu.kotatsu.utils.image.CoverSizeResolver

fun mangaListDetailedItemAD(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	clickListener: MangaDetailsClickListener,
) = adapterDelegateViewBinding<MangaListDetailedModel, ListModel, ItemMangaListDetailsBinding>(
	{ inflater, parent -> ItemMangaListDetailsBinding.inflate(inflater, parent, false) },
) {
	var badge: BadgeDrawable? = null

	val listenerAdapter = object : View.OnClickListener, View.OnLongClickListener, ChipsView.OnChipClickListener {
		override fun onClick(v: View) = when (v.id) {
			R.id.button_read -> clickListener.onReadClick(item.manga, v)
			else -> clickListener.onItemClick(item.manga, v)
		}

		override fun onLongClick(v: View): Boolean = clickListener.onItemLongClick(item.manga, v)

		override fun onChipClick(chip: Chip, data: Any?) {
			val tag = data as? MangaTag ?: return
			clickListener.onTagClick(item.manga, tag, chip)
		}
	}
	itemView.setOnClickListener(listenerAdapter)
	itemView.setOnLongClickListener(listenerAdapter)
	binding.buttonRead.setOnClickListener(listenerAdapter)
	binding.chipsTags.onChipClickListener = listenerAdapter

	bind { payloads ->
		binding.textViewTitle.text = item.title
		binding.textViewSubtitle.textAndVisible = item.subtitle
		binding.progressView.setPercent(item.progress, MangaListAdapter.PAYLOAD_PROGRESS in payloads)
		binding.imageViewCover.newImageRequest(lifecycleOwner, item.coverUrl)?.run {
			size(CoverSizeResolver(binding.imageViewCover))
			placeholder(R.drawable.ic_placeholder)
			fallback(R.drawable.ic_placeholder)
			error(R.drawable.ic_error_placeholder)
			allowRgb565(true)
			source(item.source)
			enqueueWith(coil)
		}
		binding.chipsTags.setChips(item.tags)
		binding.ratingBar.isVisible = item.manga.hasRating
		binding.ratingBar.rating = binding.ratingBar.numStars * item.manga.rating
		badge = itemView.bindBadge(badge, item.counter)
	}

	onViewRecycled {
		itemView.clearBadge(badge)
		binding.progressView.percent = PROGRESS_NONE
		badge = null
		binding.imageViewCover.disposeImageRequest()
	}
}
