package org.koitharu.kotatsu.search.ui.suggestion.adapter

import androidx.core.view.updatePadding
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.request.Disposable
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegate
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.list.decor.SpacingItemDecoration
import org.koitharu.kotatsu.databinding.ItemSearchSuggestionMangaGridBinding
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.search.ui.suggestion.SearchSuggestionListener
import org.koitharu.kotatsu.search.ui.suggestion.model.SearchSuggestionItem
import org.koitharu.kotatsu.utils.RecyclerViewScrollCallback
import org.koitharu.kotatsu.utils.ext.enqueueWith
import org.koitharu.kotatsu.utils.ext.newImageRequest

fun searchSuggestionMangaListAD(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	listener: SearchSuggestionListener,
) = adapterDelegate<SearchSuggestionItem.MangaList, SearchSuggestionItem>(R.layout.item_search_suggestion_manga_list) {

	val adapter = AsyncListDifferDelegationAdapter(
		SuggestionMangaDiffCallback(),
		searchSuggestionMangaGridAD(coil, lifecycleOwner, listener),
	)
	val recyclerView = itemView as RecyclerView
	recyclerView.adapter = adapter
	val spacing = context.resources.getDimensionPixelOffset(R.dimen.search_suggestions_manga_spacing)
	recyclerView.updatePadding(
		left = recyclerView.paddingLeft - spacing,
		right = recyclerView.paddingRight - spacing,
	)
	recyclerView.addItemDecoration(SpacingItemDecoration(spacing))
	val scrollResetCallback = RecyclerViewScrollCallback(recyclerView, 0, 0)

	bind {
		adapter.setItems(item.items, scrollResetCallback)
	}
}

private fun searchSuggestionMangaGridAD(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	listener: SearchSuggestionListener,
) = adapterDelegateViewBinding<Manga, Manga, ItemSearchSuggestionMangaGridBinding>(
	{ layoutInflater, parent -> ItemSearchSuggestionMangaGridBinding.inflate(layoutInflater, parent, false) }
) {

	var imageRequest: Disposable? = null

	itemView.setOnClickListener {
		listener.onMangaClick(item)
	}

	bind {
		imageRequest?.dispose()
		imageRequest = binding.imageViewCover.newImageRequest(item.coverUrl)
			.placeholder(R.drawable.ic_placeholder)
			.fallback(R.drawable.ic_placeholder)
			.error(R.drawable.ic_placeholder)
			.allowRgb565(true)
			.lifecycle(lifecycleOwner)
			.enqueueWith(coil)
		binding.textViewTitle.text = item.title
	}

	onViewRecycled {
		imageRequest?.dispose()
		binding.imageViewCover.setImageDrawable(null)
	}
}

private class SuggestionMangaDiffCallback : DiffUtil.ItemCallback<Manga>() {

	override fun areItemsTheSame(oldItem: Manga, newItem: Manga): Boolean {
		return oldItem.id == newItem.id
	}

	override fun areContentsTheSame(oldItem: Manga, newItem: Manga): Boolean {
		return oldItem.title == newItem.title && oldItem.coverUrl == newItem.coverUrl
	}

}