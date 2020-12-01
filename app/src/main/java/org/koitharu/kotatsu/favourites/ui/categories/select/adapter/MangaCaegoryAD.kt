package org.koitharu.kotatsu.favourites.ui.categories.select.adapter

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.databinding.ItemCategoryCheckableBinding
import org.koitharu.kotatsu.favourites.ui.categories.select.model.MangaCategoryItem

fun mangaCategoryAD(
	clickListener: OnListItemClickListener<MangaCategoryItem>
) = adapterDelegateViewBinding<MangaCategoryItem, MangaCategoryItem, ItemCategoryCheckableBinding>(
	{ inflater, parent -> ItemCategoryCheckableBinding.inflate(inflater, parent, false) }
) {

	itemView.setOnClickListener {
		clickListener.onItemClick(item, itemView)
	}

	bind {
		with(binding.checkedTextView) {
			text = item.name
			isChecked = item.isChecked
		}
	}
}