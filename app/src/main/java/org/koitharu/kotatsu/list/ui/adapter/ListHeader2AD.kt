package org.koitharu.kotatsu.list.ui.adapter

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.core.ui.titleRes
import org.koitharu.kotatsu.databinding.ItemHeader2Binding
import org.koitharu.kotatsu.list.ui.model.ListHeader2
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.utils.ext.setTextAndVisible

fun listHeader2AD(
	listener: MangaListListener,
) = adapterDelegateViewBinding<ListHeader2, ListModel, ItemHeader2Binding>(
	{ layoutInflater, parent -> ItemHeader2Binding.inflate(layoutInflater, parent, false) }
) {

	var ignoreChecking = false
	binding.textViewFilter.setOnClickListener {
		listener.onFilterClick()
	}
	binding.chipsTags.setOnCheckedStateChangeListener { _, _ ->
		if (!ignoreChecking) {
			listener.onUpdateFilter(binding.chipsTags.getCheckedData(MangaTag::class.java))
		}
	}

	bind { payloads ->
		if (payloads.isNotEmpty()) {
			binding.scrollView.smoothScrollTo(0, 0)
		}
		ignoreChecking = true
		binding.chipsTags.setChips(item.chips)
		ignoreChecking = false
		binding.textViewFilter.setTextAndVisible(item.sortOrder?.titleRes ?: 0)
	}
}