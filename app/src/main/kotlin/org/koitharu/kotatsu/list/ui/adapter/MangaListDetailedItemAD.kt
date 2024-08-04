package org.koitharu.kotatsu.list.ui.adapter

import android.view.View
import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import com.google.android.material.badge.BadgeDrawable
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.core.ui.image.CoverSizeResolver
import org.koitharu.kotatsu.core.ui.image.TrimTransformation
import org.koitharu.kotatsu.core.util.ext.defaultPlaceholders
import org.koitharu.kotatsu.core.util.ext.enqueueWith
import org.koitharu.kotatsu.core.util.ext.newImageRequest
import org.koitharu.kotatsu.core.util.ext.setOnContextClickListenerCompat
import org.koitharu.kotatsu.core.util.ext.source
import org.koitharu.kotatsu.core.util.ext.textAndVisible
import org.koitharu.kotatsu.databinding.ItemMangaListDetailsBinding
import org.koitharu.kotatsu.list.ui.ListModelDiffCallback
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.model.MangaDetailedListModel

fun mangaListDetailedItemAD(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	clickListener: MangaDetailsClickListener,
) = adapterDelegateViewBinding<MangaDetailedListModel, ListModel, ItemMangaListDetailsBinding>(
	{ inflater, parent -> ItemMangaListDetailsBinding.inflate(inflater, parent, false) },
) {
	var badge: BadgeDrawable? = null

	val listenerAdapter = object : View.OnClickListener, View.OnLongClickListener {
		override fun onClick(v: View) = clickListener.onItemClick(item.manga, v)

		override fun onLongClick(v: View): Boolean = clickListener.onItemLongClick(item.manga, v)
	}
	itemView.setOnClickListener(listenerAdapter)
	itemView.setOnLongClickListener(listenerAdapter)
	itemView.setOnContextClickListenerCompat(listenerAdapter)

	bind { payloads ->
		binding.textViewTitle.text = item.title
		binding.textViewAuthor.textAndVisible = item.manga.author
		binding.progressView.setProgress(
			value = item.progress,
			animate = ListModelDiffCallback.PAYLOAD_PROGRESS_CHANGED in payloads,
		)
		binding.imageViewCover.newImageRequest(lifecycleOwner, item.coverUrl)?.run {
			size(CoverSizeResolver(binding.imageViewCover))
			defaultPlaceholders(context)
			transformations(TrimTransformation())
			allowRgb565(true)
			tag(item.manga)
			source(item.source)
			enqueueWith(coil)
		}
		binding.textViewTags.text = item.tags.joinToString(separator = ", ") { it.title ?: "" }
		badge = itemView.bindBadge(badge, item.counter)
	}
}
