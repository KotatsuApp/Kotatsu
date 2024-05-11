package org.koitharu.kotatsu.favourites.ui.container

import android.content.Context
import android.content.Intent
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.favourites.ui.categories.FavouriteCategoriesActivity

class FavouritesContainerMenuProvider(
	private val context: Context,
) : MenuProvider {

	override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
		menuInflater.inflate(R.menu.opt_favourites_container, menu)
	}

	override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
		when (menuItem.itemId) {
			R.id.action_manage -> {
				context.startActivity(Intent(context, FavouriteCategoriesActivity::class.java))
			}

			else -> return false
		}
		return true
	}
}
