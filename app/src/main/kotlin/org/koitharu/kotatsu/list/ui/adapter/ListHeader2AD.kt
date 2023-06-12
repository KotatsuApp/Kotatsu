package org.koitharu.kotatsu.list.ui.adapter

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.core.util.ext.isAnimationsEnabled
import org.koitharu.kotatsu.databinding.FragmentFilterHeaderBinding
import org.koitharu.kotatsu.filter.ui.model.FilterHeaderModel
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.parsers.model.MangaTag

@Deprecated("")
fun listHeader2AD(
	listener: MangaListListener,
) = adapterDelegateViewBinding<FilterHeaderModel, ListModel, FragmentFilterHeaderBinding>(
	{ layoutInflater, parent -> FragmentFilterHeaderBinding.inflate(layoutInflater, parent, false) },
) {

	var ignoreChecking = false
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
	}
}
