package org.koitharu.kotatsu.favourites.ui.categories.adapter

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.databinding.ItemCategoriesAllBinding
import org.koitharu.kotatsu.favourites.ui.categories.AllCategoriesToggleListener

fun allCategoriesAD(
	allCategoriesToggleListener: AllCategoriesToggleListener,
) = adapterDelegateViewBinding<CategoryListModel.All, CategoryListModel, ItemCategoriesAllBinding>(
	{ inflater, parent -> ItemCategoriesAllBinding.inflate(inflater, parent, false) }
) {

	binding.imageViewToggle.setOnClickListener {
		allCategoriesToggleListener.onAllCategoriesToggle(!item.isVisible)
	}

	bind {
		binding.imageViewToggle.isChecked = item.isVisible
	}
}