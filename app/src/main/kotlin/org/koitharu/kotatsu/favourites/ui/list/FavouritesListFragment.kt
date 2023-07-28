package org.koitharu.kotatsu.favourites.ui.list

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.list.ListSelectionController
import org.koitharu.kotatsu.core.ui.model.titleRes
import org.koitharu.kotatsu.core.ui.util.MenuInvalidator
import org.koitharu.kotatsu.core.util.ext.addMenuProvider
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.withArgs
import org.koitharu.kotatsu.databinding.FragmentListBinding
import org.koitharu.kotatsu.favourites.ui.categories.FavouriteCategoriesActivity
import org.koitharu.kotatsu.list.ui.MangaListFragment
import org.koitharu.kotatsu.parsers.model.MangaSource

@AndroidEntryPoint
class FavouritesListFragment : MangaListFragment(), PopupMenu.OnMenuItemClickListener {

	override val viewModel by viewModels<FavouritesListViewModel>()

	override val isSwipeRefreshEnabled = false

	override fun onViewBindingCreated(binding: FragmentListBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		if (viewModel.categoryId != NO_ID) {
			addMenuProvider(FavouritesListMenuProvider(binding.root.context, viewModel))
		}
		viewModel.sortOrder.observe(viewLifecycleOwner, MenuInvalidator(requireActivity()))
	}

	override fun onScrolledToEnd() = Unit

	override fun onFilterClick(view: View?) {
		val menu = PopupMenu(view?.context ?: return, view)
		menu.setOnMenuItemClickListener(this)
		for ((i, item) in FavouriteCategoriesActivity.SORT_ORDERS.withIndex()) {
			menu.menu.add(Menu.NONE, Menu.NONE, i, item.titleRes)
		}
		menu.show()
	}

	override fun onMenuItemClick(item: MenuItem): Boolean {
		val order = FavouriteCategoriesActivity.SORT_ORDERS.getOrNull(item.order) ?: return false
		viewModel.setSortOrder(order)
		return true
	}

	override fun onCreateActionMode(controller: ListSelectionController, mode: ActionMode, menu: Menu): Boolean {
		mode.menuInflater.inflate(R.menu.mode_favourites, menu)
		return super.onCreateActionMode(controller, mode, menu)
	}

	override fun onPrepareActionMode(controller: ListSelectionController, mode: ActionMode, menu: Menu): Boolean {
		menu.findItem(R.id.action_save)?.isVisible = selectedItems.none {
			it.source == MangaSource.LOCAL
		}
		return super.onPrepareActionMode(controller, mode, menu)
	}

	override fun onActionItemClicked(controller: ListSelectionController, mode: ActionMode, item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.action_remove -> {
				viewModel.removeFromFavourites(selectedItemsIds)
				mode.finish()
				true
			}

			else -> super.onActionItemClicked(controller, mode, item)
		}
	}

	companion object {

		const val NO_ID = 0L
		const val ARG_CATEGORY_ID = "category_id"

		fun newInstance(categoryId: Long) = FavouritesListFragment().withArgs(1) {
			putLong(ARG_CATEGORY_ID, categoryId)
		}
	}
}
