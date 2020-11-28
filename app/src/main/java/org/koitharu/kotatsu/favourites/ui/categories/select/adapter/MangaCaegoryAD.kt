package org.koitharu.kotatsu.favourites.ui.categories.select.adapter

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateLayoutContainer
import kotlinx.android.synthetic.main.item_category_checkable.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.favourites.ui.categories.select.model.MangaCategoryItem

fun mangaCategoryAD(
	clickListener: OnListItemClickListener<MangaCategoryItem>
) = adapterDelegateLayoutContainer<MangaCategoryItem, MangaCategoryItem>(
	R.layout.item_category_checkable
) {

	itemView.setOnClickListener {
		clickListener.onItemClick(item, itemView)
	}

	bind {
		checkedTextView.text = item.name
		checkedTextView.isChecked = item.isChecked
	}
}