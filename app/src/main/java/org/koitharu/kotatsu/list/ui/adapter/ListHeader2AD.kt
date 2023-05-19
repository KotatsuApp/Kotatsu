package org.koitharu.kotatsu.list.ui.adapter

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.core.ui.model.titleRes
import org.koitharu.kotatsu.core.util.ext.isAnimationsEnabled
import org.koitharu.kotatsu.core.util.ext.setTextAndVisible
import org.koitharu.kotatsu.databinding.ItemHeader2Binding
import org.koitharu.kotatsu.list.ui.model.ListHeader2
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.parsers.model.MangaTag

fun listHeader2AD(
	listener: MangaListListener,
) = adapterDelegateViewBinding<ListHeader2, ListModel, ItemHeader2Binding>(
	{ layoutInflater, parent -> ItemHeader2Binding.inflate(layoutInflater, parent, false) },
) {

	var ignoreChecking = false
	binding.textViewFilter.setOnClickListener {
		listener.onFilterClick(it)
	}
	binding.chipsTags.setOnCheckedStateChangeListener { _, _ ->
		if (!ignoreChecking) {
			listener.onUpdateFilter(binding.chipsTags.getCheckedData(MangaTag::class.java))
		}
	}

	bind { payloads ->
		if (payloads.isNotEmpty()) {
			if (context.isAnimationsEnabled) {
				binding.scrollView.smoothScrollTo(0, 0)
			} else {
				binding.scrollView.scrollTo(0, 0)
			}
		}
		ignoreChecking = true
		binding.chipsTags.setChips(item.chips) // TODO use recyclerview
		ignoreChecking = false
		binding.textViewFilter.setTextAndVisible(item.sortOrder?.titleRes ?: 0)
	}
}
