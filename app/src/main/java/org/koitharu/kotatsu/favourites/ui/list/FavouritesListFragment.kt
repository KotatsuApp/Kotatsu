package org.koitharu.kotatsu.favourites.ui.list

import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.list.ui.MangaListFragment
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.utils.ext.withArgs

class FavouritesListFragment : MangaListFragment() {

	override val viewModel by viewModel<FavouritesListViewModel> {
		parametersOf(categoryId)
	}

	private val categoryId: Long
		get() = arguments?.getLong(ARG_CATEGORY_ID) ?: 0L

	override val isSwipeRefreshEnabled = false

	override fun onScrolledToEnd() = Unit

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

		private const val ARG_CATEGORY_ID = "category_id"

		fun newInstance(categoryId: Long) = FavouritesListFragment().withArgs(1) {
			putLong(ARG_CATEGORY_ID, categoryId)
		}
	}
}