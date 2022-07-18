package org.koitharu.kotatsu.favourites.ui.categories

import androidx.recyclerview.widget.RecyclerView
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.model.FavouriteCategory

interface FavouriteCategoriesListListener : OnListItemClickListener<FavouriteCategory> {

	fun onDragHandleTouch(holder: RecyclerView.ViewHolder): Boolean
}