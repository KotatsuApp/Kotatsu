package org.koitharu.kotatsu.ui.main.list.favourites

import org.koitharu.kotatsu.core.model.FavouriteCategory

interface OnCategoryCheckListener {

	fun onCategoryChecked(category: FavouriteCategory)

	fun onCategoryUnchecked(category: FavouriteCategory)
}