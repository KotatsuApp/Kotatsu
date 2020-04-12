package org.koitharu.kotatsu.ui.main.list.favourites.categories

import android.view.ViewGroup
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.ui.common.list.BaseRecyclerAdapter
import org.koitharu.kotatsu.ui.common.list.OnRecyclerItemClickListener
import org.koitharu.kotatsu.ui.main.list.favourites.categories.select.CategoryCheckableHolder

class CategoriesAdapter(onItemClickListener: OnRecyclerItemClickListener<FavouriteCategory>? = null) :
	BaseRecyclerAdapter<FavouriteCategory, Unit>(onItemClickListener) {

	override fun onCreateViewHolder(parent: ViewGroup) = CategoryHolder(parent)

	override fun onGetItemId(item: FavouriteCategory) = item.id

	override fun getExtra(item: FavouriteCategory, position: Int) = Unit
}