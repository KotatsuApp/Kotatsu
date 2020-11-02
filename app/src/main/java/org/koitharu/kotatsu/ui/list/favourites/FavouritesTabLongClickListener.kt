package org.koitharu.kotatsu.ui.list.favourites

import android.view.View
import org.koitharu.kotatsu.core.model.FavouriteCategory

fun interface FavouritesTabLongClickListener {

	fun onTabLongClick(tabView: View, category: FavouriteCategory): Boolean
}