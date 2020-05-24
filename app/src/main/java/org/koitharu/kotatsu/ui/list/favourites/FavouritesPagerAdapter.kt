package org.koitharu.kotatsu.ui.list.favourites

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.ui.common.list.AdapterUpdater
import org.koitharu.kotatsu.utils.ext.replaceWith

class FavouritesPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment), TabLayoutMediator.TabConfigurationStrategy {

	private val dataSet = ArrayList<FavouriteCategory>()

	override fun getItemCount() = dataSet.size

	override fun createFragment(position: Int): Fragment {
		val item = dataSet[position]
		return FavouritesListFragment.newInstance(item.id)
	}

	override fun onConfigureTab(tab: TabLayout.Tab, position: Int) {
		val item = dataSet[position]
		tab.text = item.title
	}

	fun replaceData(data: List<FavouriteCategory>) {
		val updater = AdapterUpdater(dataSet, data, FavouriteCategory::id)
		dataSet.replaceWith(data)
		updater(this)
	}
}