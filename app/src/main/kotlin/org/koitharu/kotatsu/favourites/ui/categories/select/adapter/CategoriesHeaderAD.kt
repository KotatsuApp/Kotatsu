package org.koitharu.kotatsu.favourites.ui.categories.select.adapter

import android.content.Intent
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
			R.id.chip_manage -> Intent(v.context, FavouriteCategoriesActivity::class.java)
			else -> return@OnClickListener
		}
		v.context.startActivity(intent)
	}

	binding.chipCreate.setOnClickListener(onClickListener)
	binding.chipManage.setOnClickListener(onClickListener)
}
