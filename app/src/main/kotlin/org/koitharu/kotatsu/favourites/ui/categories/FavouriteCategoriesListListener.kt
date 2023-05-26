package org.koitharu.kotatsu.favourites.ui.categories

import androidx.recyclerview.widget.RecyclerView
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener

interface FavouriteCategoriesListListener : OnListItemClickListener<FavouriteCategory> {

	fun onDragHandleTouch(holder: RecyclerView.ViewHolder): Boolean
}
