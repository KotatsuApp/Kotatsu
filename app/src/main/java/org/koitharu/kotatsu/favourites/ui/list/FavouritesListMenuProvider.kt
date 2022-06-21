package org.koitharu.kotatsu.favourites.ui.list

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import androidx.core.view.iterator
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.titleRes
import org.koitharu.kotatsu.favourites.ui.categories.CategoriesActivity

class FavouritesListMenuProvider(
	private val viewModel: FavouritesListViewModel,
) : MenuProvider {

	override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
		menuInflater.inflate(R.menu.opt_favourites_list, menu)
		menu.findItem(R.id.action_order)?.subMenu?.let { submenu ->
			for ((i, item) in CategoriesActivity.SORT_ORDERS.withIndex()) {
				val menuItem = submenu.add(R.id.group_order, Menu.NONE, i, item.titleRes)
				menuItem.isCheckable = true
			}
			submenu.setGroupCheckable(R.id.group_order, true, true)
		}
	}

	override fun onPrepareMenu(menu: Menu) {
		menu.findItem(R.id.action_order)?.subMenu?.let { submenu ->
			val selectedOrder = viewModel.sortOrder.value
			for (item in submenu) {
				val order = CategoriesActivity.SORT_ORDERS.getOrNull(item.order)
				item.isChecked = order == selectedOrder
			}
		}
	}

	override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
		return when {
			menuItem.itemId == R.id.action_order -> false
			menuItem.groupId == R.id.group_order -> {
				val order = CategoriesActivity.SORT_ORDERS.getOrNull(menuItem.order) ?: return false
				viewModel.setSortOrder(order)
				true
			}
			else -> false
		}
	}
}