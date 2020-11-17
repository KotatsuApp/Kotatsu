package org.koitharu.kotatsu.favourites.ui

import android.view.View
import org.koitharu.kotatsu.core.model.FavouriteCategory

fun interface FavouritesTabLongClickListener {

	fun onTabLongClick(tabView: View, category: FavouriteCategory): Boolean
}