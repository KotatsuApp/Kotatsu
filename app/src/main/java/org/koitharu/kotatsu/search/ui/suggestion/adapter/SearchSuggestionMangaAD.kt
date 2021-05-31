package org.koitharu.kotatsu.search.ui.suggestion.adapter

import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import coil.request.Disposable
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.databinding.ItemSearchSuggestionMangaBinding
import org.koitharu.kotatsu.search.ui.suggestion.SearchSuggestionListener
import org.koitharu.kotatsu.search.ui.suggestion.model.SearchSuggestionItem
import org.koitharu.kotatsu.utils.ext.enqueueWith
import org.koitharu.kotatsu.utils.ext.newImageRequest
import org.koitharu.kotatsu.utils.ext.textAndVisible

fun searchSuggestionMangaAD(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	listener: SearchSuggestionListener,
) = adapterDelegateViewBinding<SearchSuggestionItem.MangaItem, SearchSuggestionItem, ItemSearchSuggestionMangaBinding>(
	{ inflater, parent -> ItemSearchSuggestionMangaBinding.inflate(inflater, parent, false) }
) {

	var imageRequest: Disposable? = null

	itemView.setOnClickListener {
		listener.onMangaClick(item.manga)
	}

	bind {
		imageRequest?.dispose()
		imageRequest = binding.imageViewCover.newImageRequest(item.manga.coverUrl)
			.placeholder(R.drawable.ic_placeholder)
			.fallback(R.drawable.ic_placeholder)
			.error(R.drawable.ic_placeholder)
			.allowRgb565(true)
			.lifecycle(lifecycleOwner)
			.enqueueWith(coil)
		binding.textViewTitle.text = item.manga.title
		binding.textViewSubtitle.textAndVisible = item.manga.altTitle
	}

	onViewRecycled {
		imageRequest?.dispose()
		binding.imageViewCover.setImageDrawable(null)
	}
}