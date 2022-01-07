package org.koitharu.kotatsu.list.ui.adapter

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.base.ui.widgets.ChipsView
import org.koitharu.kotatsu.core.model.MangaTag
import org.koitharu.kotatsu.databinding.ItemCurrentFilterBinding
import org.koitharu.kotatsu.list.ui.model.CurrentFilterModel
import org.koitharu.kotatsu.list.ui.model.ListModel

fun currentFilterAD(
	onTagRemoveClick: (MangaTag) -> Unit,
) = adapterDelegateViewBinding<CurrentFilterModel, ListModel, ItemCurrentFilterBinding>(
	{ inflater, parent -> ItemCurrentFilterBinding.inflate(inflater, parent, false) }
) {

	binding.chipsTags.onChipCloseClickListener = ChipsView.OnChipCloseClickListener { chip, data ->
		onTagRemoveClick(data as? MangaTag ?: return@OnChipCloseClickListener)
	}

	bind {
		binding.chipsTags.setChips(item.chips)
	}
}