package org.koitharu.kotatsu.favourites.ui.container

import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.AdapterListUpdateCallback
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.viewpager2.adapter.FragmentStateAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.flow.FlowCollector
import org.koitharu.kotatsu.core.util.ContinuationResumeRunnable
import org.koitharu.kotatsu.favourites.ui.list.FavouritesListFragment
import org.koitharu.kotatsu.list.ui.ListModelDiffCallback
import kotlin.coroutines.suspendCoroutine

class FavouritesContainerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment),
	FlowCollector<List<FavouriteTabModel>> {

	private val differ = AsyncListDiffer(
		AdapterListUpdateCallback(this),
		AsyncDifferConfig.Builder(ListModelDiffCallback<FavouriteTabModel>())
			.setBackgroundThreadExecutor(Dispatchers.Default.limitedParallelism(2).asExecutor())
			.build(),
	)

	override fun getItemCount(): Int = differ.currentList.size

	override fun getItemId(position: Int): Long {
		return differ.currentList[position].id
	}

	override fun containsItem(itemId: Long): Boolean {
		return differ.currentList.any { x -> x.id == itemId }
	}

	override fun createFragment(position: Int): Fragment {
		val item = differ.currentList[position]
		return FavouritesListFragment.newInstance(item.id)
	}

	override suspend fun emit(value: List<FavouriteTabModel>) = suspendCoroutine { cont ->
		differ.submitList(value, ContinuationResumeRunnable(cont))
	}

	fun getItem(position: Int): FavouriteTabModel = differ.currentList[position]
}
