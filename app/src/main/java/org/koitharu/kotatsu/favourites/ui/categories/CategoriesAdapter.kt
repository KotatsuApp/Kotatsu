package org.koitharu.kotatsu.favourites.ui.categories

import androidx.recyclerview.widget.DiffUtil
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.favourites.ui.categories.adapter.CategoryListModel
import org.koitharu.kotatsu.favourites.ui.categories.adapter.allCategoriesAD
import org.koitharu.kotatsu.favourites.ui.categories.adapter.categoryAD

class CategoriesAdapter(
	onItemClickListener: OnListItemClickListener<FavouriteCategory>,
	allCategoriesToggleListener: AllCategoriesToggleListener,
) : AsyncListDifferDelegationAdapter<CategoryListModel>(DiffCallback()) {

	init {
		delegatesManager.addDelegate(categoryAD(onItemClickListener))
			.addDelegate(allCategoriesAD(allCategoriesToggleListener))
		setHasStableIds(true)
	}

	override fun getItemId(position: Int): Long {
		return items[position].id
	}

	private class DiffCallback : DiffUtil.ItemCallback<CategoryListModel>() {

		override fun areItemsTheSame(
			oldItem: CategoryListModel,
			newItem: CategoryListModel,
		): Boolean = oldItem.id == newItem.id

		override fun areContentsTheSame(
			oldItem: CategoryListModel,
			newItem: CategoryListModel,
		): Boolean = oldItem == newItem

		override fun getChangePayload(
			oldItem: CategoryListModel,
			newItem: CategoryListModel,
		): Any? = when {
			oldItem is CategoryListModel.All && newItem is CategoryListModel.All -> Unit
			else -> super.getChangePayload(oldItem, newItem)
		}
	}
}