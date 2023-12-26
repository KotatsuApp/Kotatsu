package org.koitharu.kotatsu.favourites.ui.categories.select.adapter

import android.view.View
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.databinding.ItemCategoriesHeaderBinding
import org.koitharu.kotatsu.favourites.ui.categories.FavouriteCategoriesActivity
import org.koitharu.kotatsu.favourites.ui.categories.edit.FavouritesCategoryEditActivity
import org.koitharu.kotatsu.favourites.ui.categories.select.model.CategoriesHeaderItem
import org.koitharu.kotatsu.list.ui.model.ListModel

fun categoriesHeaderAD() = adapterDelegateViewBinding<CategoriesHeaderItem, ListModel, ItemCategoriesHeaderBinding>(
	{ inflater, parent -> ItemCategoriesHeaderBinding.inflate(inflater, parent, false) },
) {

	val onClickListener = View.OnClickListener { v ->
		val intent = when (v.id) {
			R.id.chip_create -> FavouritesCategoryEditActivity.newIntent(v.context)
			R.id.chip_manage -> FavouriteCategoriesActivity.newIntent(v.context)
			else -> return@OnClickListener
		}
		v.context.startActivity(intent)
	}

	binding.chipCreate.setOnClickListener(onClickListener)
	binding.chipManage.setOnClickListener(onClickListener)
}
