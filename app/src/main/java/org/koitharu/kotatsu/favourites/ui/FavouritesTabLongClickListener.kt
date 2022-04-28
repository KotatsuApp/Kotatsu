package org.koitharu.kotatsu.favourites.ui

import android.view.View
import org.koitharu.kotatsu.favourites.ui.categories.adapter.CategoryListModel

fun interface FavouritesTabLongClickListener {

	fun onTabLongClick(tabView: View, item: CategoryListModel): Boolean
}