package org.koitharu.kotatsu.search.ui.suggestion.adapter

import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import org.koitharu.kotatsu.search.ui.suggestion.SearchSuggestionListener
import org.koitharu.kotatsu.search.ui.suggestion.model.SearchSuggestionItem
import kotlin.jvm.internal.Intrinsics

class SearchSuggestionAdapter(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	listener: SearchSuggestionListener,
) : AsyncListDifferDelegationAdapter<SearchSuggestionItem>(DiffCallback()) {

	init {
		delegatesManager.addDelegate(ITEM_TYPE_MANGA, searchSuggestionMangaAD(coil, lifecycleOwner, listener))
			.addDelegate(ITEM_TYPE_QUERY, searchSuggestionQueryAD(listener))
			.addDelegate(ITEM_TYPE_HEADER, searchSuggestionHeaderAD(listener))
			.addDelegate(ITEM_TYPE_TAGS, searchSuggestionTagsAD(listener))
	}

	private class DiffCallback : DiffUtil.ItemCallback<SearchSuggestionItem>() {

		override fun areItemsTheSame(
			oldItem: SearchSuggestionItem,
			newItem: SearchSuggestionItem,
		): Boolean = when {
			oldItem is SearchSuggestionItem.MangaItem && newItem is SearchSuggestionItem.MangaItem -> {
				oldItem.manga.id == newItem.manga.id
			}
			oldItem is SearchSuggestionItem.RecentQuery && newItem is SearchSuggestionItem.RecentQuery -> {
				oldItem.query == newItem.query
			}
			oldItem is SearchSuggestionItem.Header && newItem is SearchSuggestionItem.Header -> true
			oldItem is SearchSuggestionItem.Tags && newItem is SearchSuggestionItem.Tags -> true
			else -> false
		}

		override fun areContentsTheSame(
			oldItem: SearchSuggestionItem,
			newItem: SearchSuggestionItem,
		): Boolean = Intrinsics.areEqual(oldItem, newItem)
	}

	companion object {

		const val ITEM_TYPE_MANGA = 0
		const val ITEM_TYPE_QUERY = 1
		const val ITEM_TYPE_HEADER = 2
		const val ITEM_TYPE_TAGS = 3
	}
}