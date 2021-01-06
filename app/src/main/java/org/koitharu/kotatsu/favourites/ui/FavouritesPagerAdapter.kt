package org.koitharu.kotatsu.favourites.ui

import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.favourites.ui.list.FavouritesListFragment

class FavouritesPagerAdapter(
	fragment: Fragment,
	private val longClickListener: FavouritesTabLongClickListener
) : FragmentStateAdapter(fragment.childFragmentManager, fragment.viewLifecycleOwner.lifecycle),
	TabLayoutMediator.TabConfigurationStrategy, View.OnLongClickListener {

	private val differ = AsyncListDiffer(this, DiffCallback())

	override fun getItemCount() = differ.currentList.size

	override fun createFragment(position: Int): Fragment {
		val item = differ.currentList[position]
		return FavouritesListFragment.newInstance(item.id)
	}

	override fun onConfigureTab(tab: TabLayout.Tab, position: Int) {
		val item = differ.currentList[position]
		tab.text = item.title
		tab.view.tag = item
		tab.view.setOnLongClickListener(this)
	}

	fun replaceData(data: List<FavouriteCategory>) {
		differ.submitList(data)
	}

	override fun onLongClick(v: View): Boolean {
		val item = v.tag as? FavouriteCategory ?: return false
		return longClickListener.onTabLongClick(v, item)
	}

	private class DiffCallback : DiffUtil.ItemCallback<FavouriteCategory>() {

		override fun areItemsTheSame(
			oldItem: FavouriteCategory,
			newItem: FavouriteCategory
		): Boolean = oldItem.id == newItem.id

		override fun areContentsTheSame(
			oldItem: FavouriteCategory,
			newItem: FavouriteCategory
		): Boolean = oldItem.id == newItem.id && oldItem.title == newItem.title
	}
}