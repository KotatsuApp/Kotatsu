package org.koitharu.kotatsu.list.ui.adapter

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.databinding.ItemEmptyStateBinding
import org.koitharu.kotatsu.list.ui.model.EmptyState
import org.koitharu.kotatsu.list.ui.model.ListModel

fun emptyStateListAD() = adapterDelegateViewBinding<EmptyState, ListModel, ItemEmptyStateBinding>(
	{ inflater, parent -> ItemEmptyStateBinding.inflate(inflater, parent, false) }
) {

	bind {
		with(binding.icon) {
			setImageResource(item.icon)
		}
		with(binding.textPrimary) {
			setText(item.textPrimary)
		}
		with(binding.textSecondary) {
			setText(item.textSecondary)
		}
	}
}