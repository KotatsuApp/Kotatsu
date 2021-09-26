package org.koitharu.kotatsu.search.ui.suggestion.adapter

import android.view.View
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.databinding.ItemSearchSuggestionQueryBinding
import org.koitharu.kotatsu.search.ui.suggestion.SearchSuggestionListener
import org.koitharu.kotatsu.search.ui.suggestion.model.SearchSuggestionItem

fun searchSuggestionQueryAD(
	listener: SearchSuggestionListener,
) = adapterDelegateViewBinding<SearchSuggestionItem.RecentQuery, SearchSuggestionItem, ItemSearchSuggestionQueryBinding>(
	{ inflater, parent -> ItemSearchSuggestionQueryBinding.inflate(inflater, parent, false) }
) {

	val viewClickListener = View.OnClickListener { v ->
		listener.onQueryClick(item.query, v.id != R.id.button_complete)
	}

	binding.root.setOnClickListener(viewClickListener)
	binding.buttonComplete.setOnClickListener(viewClickListener)

	bind {
		binding.textViewTitle.text = item.query
	}
}