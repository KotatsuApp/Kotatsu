package org.koitharu.kotatsu.favourites.ui.list

import android.content.Context
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import androidx.core.view.forEach
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.model.titleRes
import org.koitharu.kotatsu.favourites.ui.categories.FavouriteCategoriesActivity
import org.koitharu.kotatsu.favourites.ui.categories.edit.FavouritesCategoryEditActivity
import org.koitharu.kotatsu.parsers.model.SortOrder

class FavouritesListMenuProvider(
	private val context: Context,
	private val viewModel: FavouritesListViewModel,
) : MenuProvider {

	override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
		menuInflater.inflate(R.menu.opt_favourites, menu)
		val subMenu = menu.findItem(R.id.action_order)?.subMenu ?: return
		for (order in FavouriteCategoriesActivity.SORT_ORDERS) {
			subMenu.add(R.id.group_order, Menu.NONE, order.ordinal, order.titleRes)
		}
		subMenu.setGroupCheckable(R.id.group_order, true, true)
	}

	override fun onPrepareMenu(menu: Menu) {
		super.onPrepareMenu(menu)
		val order = viewModel.sortOrder.value ?: return
		menu.findItem(R.id.action_order)?.subMenu?.forEach { item ->
			if (item.order == order.ordinal) {
				item.isChecked = true
			}
		}
	}

	override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
		if (menuItem.groupId == R.id.group_order) {
			val order = SortOrder.entries[menuItem.order]
			viewModel.setSortOrder(order)
			return true
		}
		return when (menuItem.itemId) {
			R.id.action_edit -> {
				context.startActivity(
					FavouritesCategoryEditActivity.newIntent(context, viewModel.categoryId),
				)
				true
			}

			else -> false
		}
	}
}
