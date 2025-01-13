package org.koitharu.kotatsu.list.ui.adapter

import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import coil3.ImageLoader
import coil3.request.allowRgb565
import coil3.request.transformations
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.core.ui.image.TrimTransformation
import org.koitharu.kotatsu.core.ui.list.AdapterDelegateClickListenerAdapter
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.util.ext.defaultPlaceholders
import org.koitharu.kotatsu.core.util.ext.enqueueWith
import org.koitharu.kotatsu.core.util.ext.mangaExtra
import org.koitharu.kotatsu.core.util.ext.newImageRequest
import org.koitharu.kotatsu.core.util.ext.textAndVisible
import org.koitharu.kotatsu.databinding.ItemMangaListBinding
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.model.MangaCompactListModel
import org.koitharu.kotatsu.parsers.model.Manga

fun mangaListItemAD(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	clickListener: OnListItemClickListener<Manga>,
) = adapterDelegateViewBinding<MangaCompactListModel, ListModel, ItemMangaListBinding>(
	{ inflater, parent -> ItemMangaListBinding.inflate(inflater, parent, false) },
) {

	AdapterDelegateClickListenerAdapter(this, clickListener, MangaCompactListModel::manga).attach(itemView)

	bind {
		binding.textViewTitle.text = item.title
		binding.textViewSubtitle.textAndVisible = item.subtitle
		binding.imageViewCover.newImageRequest(lifecycleOwner, item.coverUrl)?.run {
			defaultPlaceholders(context)
			allowRgb565(true)
			transformations(TrimTransformation())
			mangaExtra(item.manga)
			enqueueWith(coil)
		}
		binding.badge.number = item.counter
		binding.badge.isVisible = item.counter > 0
	}
}
