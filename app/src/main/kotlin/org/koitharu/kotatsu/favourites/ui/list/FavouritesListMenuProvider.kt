package org.koitharu.kotatsu.favourites.ui.list

import android.content.Context
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.favourites.ui.categories.edit.FavouritesCategoryEditActivity

class FavouritesListMenuProvider(
	private val context: Context,
	private val viewModel: FavouritesListViewModel,
) : MenuProvider {

	override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
		menuInflater.inflate(R.menu.opt_favourites, menu)
	}

	override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
		return when (menuItem.itemId) {
			R.id.action_edit -> {
				context.startActivity(FavouritesCategoryEditActivity.newIntent(context, viewModel.categoryId))
				true
			}

			else -> false
		}
	}
}
