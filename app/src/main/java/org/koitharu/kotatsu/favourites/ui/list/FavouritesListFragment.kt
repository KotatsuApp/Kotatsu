package org.koitharu.kotatsu.favourites.ui.list

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.list.ui.MangaListFragment
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.utils.ext.withArgs

class FavouritesListFragment : MangaListFragment() {

	override val viewModel by viewModel<FavouritesListViewModel> {
		parametersOf(categoryId)
	}

	private val categoryId: Long
		get() = arguments?.getLong(ARG_CATEGORY_ID) ?: 0L

	override val isSwipeRefreshEnabled = false

	override fun onScrolledToEnd() = Unit

	override fun onCreatePopupMenu(inflater: MenuInflater, menu: Menu, data: Manga) {
		super.onCreatePopupMenu(inflater, menu, data)
		inflater.inflate(R.menu.popup_favourites, menu)
	}

	override fun onPopupMenuItemSelected(item: MenuItem, data: Manga) = when (item.itemId) {
		R.id.action_remove -> {
			viewModel.removeFromFavourites(data)
			true
		}
		else -> super.onPopupMenuItemSelected(item, data)
	}

	companion object {

		private const val ARG_CATEGORY_ID = "category_id"

		fun newInstance(categoryId: Long) = FavouritesListFragment().withArgs(1) {
			putLong(ARG_CATEGORY_ID, categoryId)
		}
	}
}