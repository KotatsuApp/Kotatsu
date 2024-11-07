package org.koitharu.kotatsu.list.ui.adapter

import androidx.lifecycle.LifecycleOwner
import coil3.ImageLoader
import coil3.request.allowRgb565
import coil3.request.transformations
import com.google.android.material.badge.BadgeDrawable
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.core.ui.image.CoverSizeResolver
import org.koitharu.kotatsu.core.ui.image.TrimTransformation
import org.koitharu.kotatsu.core.ui.list.AdapterDelegateClickListenerAdapter
import org.koitharu.kotatsu.core.util.ext.defaultPlaceholders
import org.koitharu.kotatsu.core.util.ext.enqueueWith
import org.koitharu.kotatsu.core.util.ext.mangaExtra
import org.koitharu.kotatsu.core.util.ext.newImageRequest
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

	AdapterDelegateClickListenerAdapter(this, clickListener, MangaDetailedListModel::manga).attach(itemView)

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
			mangaExtra(item.manga)
			enqueueWith(coil)
		}
		binding.textViewTags.text = item.tags.joinToString(separator = ", ") { it.title ?: "" }
		badge = itemView.bindBadge(badge, item.counter)
	}
}
