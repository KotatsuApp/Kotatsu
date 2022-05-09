package org.koitharu.kotatsu.favourites.ui.list

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.view.ActionMode
import androidx.core.view.iterator
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.titleRes
import org.koitharu.kotatsu.favourites.ui.categories.CategoriesActivity
import org.koitharu.kotatsu.list.ui.MangaListFragment
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.utils.ext.withArgs

class FavouritesListFragment : MangaListFragment() {

	override val viewModel by viewModel<FavouritesListViewModel> {
		parametersOf(categoryId)
	}

	private val categoryId: Long
		get() = arguments?.getLong(ARG_CATEGORY_ID) ?: NO_ID

	override val isSwipeRefreshEnabled = false

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		viewModel.sortOrder.observe(viewLifecycleOwner) { activity?.invalidateOptionsMenu() }
	}

	override fun onScrolledToEnd() = Unit

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		super.onCreateOptionsMenu(menu, inflater)
		if (categoryId != NO_ID) {
			inflater.inflate(R.menu.opt_favourites_list, menu)
			menu.findItem(R.id.action_order)?.subMenu?.let { submenu ->
				for ((i, item) in CategoriesActivity.SORT_ORDERS.withIndex()) {
					val menuItem = submenu.add(R.id.group_order, Menu.NONE, i, item.titleRes)
					menuItem.isCheckable = true
				}
				submenu.setGroupCheckable(R.id.group_order, true, true)
			}
		}
	}

	override fun onPrepareOptionsMenu(menu: Menu) {
		super.onPrepareOptionsMenu(menu)
		menu.findItem(R.id.action_order)?.subMenu?.let { submenu ->
			val selectedOrder = viewModel.sortOrder.value
			for (item in submenu) {
				val order = CategoriesActivity.SORT_ORDERS.getOrNull(item.order)
				item.isChecked = order == selectedOrder
			}
		}
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when {
			item.itemId == R.id.action_order -> false
			item.groupId == R.id.group_order -> {
				val order = CategoriesActivity.SORT_ORDERS.getOrNull(item.order) ?: return false
				viewModel.setSortOrder(order)
				true
			}
			else -> super.onOptionsItemSelected(item)
		}
	}

	override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
		mode.menuInflater.inflate(R.menu.mode_favourites, menu)
		return super.onCreateActionMode(mode, menu)
	}

	override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
		menu.findItem(R.id.action_save)?.isVisible = selectedItems.none {
			it.source == MangaSource.LOCAL
		}
		return super.onPrepareActionMode(mode, menu)
	}

	override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.action_remove -> {
				viewModel.removeFromFavourites(selectedItemsIds)
				mode.finish()
				true
			}
			else -> super.onActionItemClicked(mode, item)
		}
	}

	companion object {

		const val NO_ID = 0L
		private const val ARG_CATEGORY_ID = "category_id"

		fun newInstance(categoryId: Long) = FavouritesListFragment().withArgs(1) {
			putLong(ARG_CATEGORY_ID, categoryId)
		}
	}
}