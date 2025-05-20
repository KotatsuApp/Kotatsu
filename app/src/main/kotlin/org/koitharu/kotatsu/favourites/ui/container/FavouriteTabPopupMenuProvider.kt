package org.koitharu.kotatsu.favourites.ui.container

import android.content.Context
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.nav.AppRouter
import org.koitharu.kotatsu.core.ui.dialog.buildAlertDialog
import org.koitharu.kotatsu.favourites.ui.list.FavouritesListFragment.Companion.NO_ID

class FavouriteTabPopupMenuProvider(
	private val context: Context,
	private val router: AppRouter,
	private val viewModel: FavouritesContainerViewModel,
	private val categoryId: Long
) : MenuProvider {

	override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
		val menuResId = if (categoryId == NO_ID) {
			R.menu.popup_fav_tab_all
		} else {
			R.menu.popup_fav_tab
		}
		menuInflater.inflate(menuResId, menu)
	}

	override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
		when (menuItem.itemId) {
			R.id.action_hide -> viewModel.hide(categoryId)
			R.id.action_edit -> router.openFavoriteCategoryEdit(categoryId)
			R.id.action_delete -> confirmDelete()
			R.id.action_manage -> router.openFavoriteCategories()
			else -> return false
		}
		return true
	}

	private fun confirmDelete() {
		buildAlertDialog(context, isCentered = true) {
			setMessage(R.string.categories_delete_confirm)
			setTitle(R.string.remove_category)
			setIcon(R.drawable.ic_delete)
			setNegativeButton(android.R.string.cancel, null)
			setPositiveButton(R.string.remove) { _, _ -> viewModel.deleteCategory(categoryId) }
		}.show()
	}
}
