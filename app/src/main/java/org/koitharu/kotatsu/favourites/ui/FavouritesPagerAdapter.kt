package org.koitharu.kotatsu.favourites.ui

import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.favourites.ui.categories.adapter.CategoryListModel
import org.koitharu.kotatsu.favourites.ui.list.FavouritesListFragment

class FavouritesPagerAdapter(
	fragment: Fragment,
	private val longClickListener: FavouritesTabLongClickListener
) : FragmentStateAdapter(fragment.childFragmentManager, fragment.viewLifecycleOwner.lifecycle),
	TabLayoutMediator.TabConfigurationStrategy,
	View.OnLongClickListener {

	private val differ = AsyncListDiffer(this, DiffCallback())

	override fun getItemCount() = differ.currentList.size

	override fun createFragment(position: Int): Fragment {
		val item = differ.currentList[position]
		return FavouritesListFragment.newInstance(item.id)
	}

	override fun getItemId(position: Int): Long {
		return differ.currentList[position].id
	}

	override fun containsItem(itemId: Long): Boolean {
		return differ.currentList.any { it.id == itemId }
	}

	override fun onConfigureTab(tab: TabLayout.Tab, position: Int) {
		val item = differ.currentList[position]
		tab.text = when (item) {
			is CategoryListModel.All -> tab.view.context.getString(R.string.all_favourites)
			is CategoryListModel.CategoryItem -> item.category.title
		}
		tab.view.tag = item.id
		tab.view.setOnLongClickListener(this)
	}

	fun replaceData(data: List<CategoryListModel>) {
		differ.submitList(data)
	}

	override fun onLongClick(v: View): Boolean {
		val itemId = v.tag as? Long ?: return false
		val item = differ.currentList.find { x -> x.id == itemId } ?: return false
		return longClickListener.onTabLongClick(v, item)
	}

	private class DiffCallback : DiffUtil.ItemCallback<CategoryListModel>() {

		override fun areItemsTheSame(
			oldItem: CategoryListModel,
			newItem: CategoryListModel
		): Boolean = when {
			oldItem is CategoryListModel.All && newItem is CategoryListModel.All -> true
			oldItem is CategoryListModel.CategoryItem && newItem is CategoryListModel.CategoryItem -> {
				oldItem.category.id == newItem.category.id
			}
			else -> false
		}

		override fun areContentsTheSame(
			oldItem: CategoryListModel,
			newItem: CategoryListModel
		): Boolean = oldItem == newItem
	}
}