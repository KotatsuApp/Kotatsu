package org.koitharu.kotatsu.ui.list.favourites.categories.select

import org.koitharu.kotatsu.core.model.FavouriteCategory

interface OnCategoryCheckListener {

	fun onCategoryChecked(category: FavouriteCategory)

	fun onCategoryUnchecked(category: FavouriteCategory)
}