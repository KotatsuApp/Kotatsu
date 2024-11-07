package org.koitharu.kotatsu.scrobbling.common.ui.config.adapter

import androidx.lifecycle.LifecycleOwner
import coil3.ImageLoader
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.core.ui.list.AdapterDelegateClickListenerAdapter
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.util.ext.defaultPlaceholders
import org.koitharu.kotatsu.core.util.ext.enqueueWith
import org.koitharu.kotatsu.core.util.ext.newImageRequest
import org.koitharu.kotatsu.databinding.ItemScrobblingMangaBinding
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblingInfo

fun scrobblingMangaAD(
	clickListener: OnListItemClickListener<ScrobblingInfo>,
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
) = adapterDelegateViewBinding<ScrobblingInfo, ListModel, ItemScrobblingMangaBinding>(
	{ layoutInflater, parent -> ItemScrobblingMangaBinding.inflate(layoutInflater, parent, false) },
) {

	AdapterDelegateClickListenerAdapter(this, clickListener).attach(itemView)

	bind {
		binding.imageViewCover.newImageRequest(lifecycleOwner, item.coverUrl)?.run {
			defaultPlaceholders(context)
			enqueueWith(coil)
		}
		binding.textViewTitle.text = item.title
		binding.ratingBar.rating = item.rating * binding.ratingBar.numStars
	}
}
