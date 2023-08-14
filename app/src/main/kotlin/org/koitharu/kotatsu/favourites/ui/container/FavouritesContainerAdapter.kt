package org.koitharu.kotatsu.favourites.ui.container

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import kotlinx.coroutines.flow.FlowCollector
import org.koitharu.kotatsu.favourites.ui.list.FavouritesListFragment
import org.koitharu.kotatsu.parsers.util.replaceWith

@Suppress("DEPRECATION")
class FavouritesContainerAdapter(
	fm: FragmentManager
) : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT),
	FlowCollector<List<FavouriteTabModel>> {

	private val dataSet = ArrayList<FavouriteTabModel>()

	override fun getCount(): Int = dataSet.size

	override fun getItem(position: Int): Fragment {
		val item = dataSet[position]
		return FavouritesListFragment.newInstance(item.id)
	}

	override fun getPageTitle(position: Int): CharSequence {
		return dataSet[position].title
	}

	override suspend fun emit(value: List<FavouriteTabModel>) {
		dataSet.replaceWith(value)
		notifyDataSetChanged()
	}
}
