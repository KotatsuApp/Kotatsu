package org.koitharu.kotatsu.widget.shelf.adapter

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateLayoutContainer
import kotlinx.android.synthetic.main.item_category_checkable.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.widget.shelf.model.CategoryItem

fun categorySelectItemAD(
	clickListener: OnListItemClickListener<CategoryItem>
) = adapterDelegateLayoutContainer<CategoryItem, CategoryItem>(
	R.layout.item_category_checkable_single
) {

	itemView.setOnClickListener {
		clickListener.onItemClick(item, it)
	}

	bind {
		checkedTextView.text = item.name ?: getString(R.string.all_favourites)
		checkedTextView.isChecked = item.isSelected
	}
}