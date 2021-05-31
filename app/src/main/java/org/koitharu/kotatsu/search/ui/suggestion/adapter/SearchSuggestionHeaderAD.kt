package org.koitharu.kotatsu.search.ui.suggestion.adapter

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.databinding.ItemSearchSuggestionHeaderBinding
import org.koitharu.kotatsu.search.ui.suggestion.SearchSuggestionListener
import org.koitharu.kotatsu.search.ui.suggestion.model.SearchSuggestionItem

fun searchSuggestionHeaderAD(
	listener: SearchSuggestionListener,
) = adapterDelegateViewBinding<SearchSuggestionItem.Header, SearchSuggestionItem, ItemSearchSuggestionHeaderBinding>(
		{ inflater, parent -> ItemSearchSuggestionHeaderBinding.inflate(inflater, parent, false) }
	) {

		binding.switchLocal.setOnCheckedChangeListener { _, isChecked ->
			item.isChecked.value = isChecked
		}
		binding.buttonClear.setOnClickListener {
			listener.onClearSearchHistory()
		}

		bind {
			binding.switchLocal.text = getString(
				R.string.search_only_on_s,
				item.source.title,
			)
			binding.switchLocal.isChecked = item.isChecked.value
		}
	}