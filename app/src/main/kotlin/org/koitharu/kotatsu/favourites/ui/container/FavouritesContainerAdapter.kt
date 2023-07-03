package org.koitharu.kotatsu.favourites.ui.container

import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator.TabConfigurationStrategy
import kotlinx.coroutines.flow.FlowCollector
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.core.util.ContinuationResumeRunnable
import org.koitharu.kotatsu.favourites.ui.list.FavouritesListFragment
import kotlin.coroutines.suspendCoroutine

class FavouritesContainerAdapter(fragment: Fragment) : FragmentStateAdapter(
	fragment.childFragmentManager,
	fragment.viewLifecycleOwner.lifecycle,
),
	TabConfigurationStrategy,
	FlowCollector<List<FavouriteCategory>> {

	private val differ = AsyncListDiffer(this, DiffCallback())

	override fun getItemCount(): Int = differ.currentList.size

	override fun getItemId(position: Int): Long {
		return differ.currentList[position].id
	}

	override fun createFragment(position: Int): Fragment {
		return FavouritesListFragment.newInstance(getItemId(position))
	}

	override fun onConfigureTab(tab: TabLayout.Tab, position: Int) {
		val item = differ.currentList[position]
		tab.text = item.title
		tab.tag = item
	}

	override suspend fun emit(value: List<FavouriteCategory>) = suspendCoroutine { cont ->
		differ.submitList(value, ContinuationResumeRunnable(cont))
	}

	private class DiffCallback : DiffUtil.ItemCallback<FavouriteCategory>() {

		override fun areItemsTheSame(oldItem: FavouriteCategory, newItem: FavouriteCategory): Boolean {
			return oldItem.id == newItem.id
		}

		override fun areContentsTheSame(oldItem: FavouriteCategory, newItem: FavouriteCategory): Boolean {
			return oldItem.title == newItem.title
		}
	}
}
