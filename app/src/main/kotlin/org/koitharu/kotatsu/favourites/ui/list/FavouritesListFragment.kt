package org.koitharu.kotatsu.favourites.ui.list

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.isLocal
import org.koitharu.kotatsu.core.ui.list.ListSelectionController
import org.koitharu.kotatsu.core.util.ext.sortedByOrdinal
import org.koitharu.kotatsu.core.util.ext.withArgs
import org.koitharu.kotatsu.databinding.FragmentListBinding
import org.koitharu.kotatsu.list.domain.ListSortOrder
import org.koitharu.kotatsu.list.ui.MangaListFragment

@AndroidEntryPoint
class FavouritesListFragment : MangaListFragment(), PopupMenu.OnMenuItemClickListener {

	override val viewModel by viewModels<FavouritesListViewModel>()

	override val isSwipeRefreshEnabled = false

	val categoryId
		get() = viewModel.categoryId

	override fun onViewBindingCreated(binding: FragmentListBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		binding.recyclerView.isVP2BugWorkaroundEnabled = true
	}

	override fun onScrolledToEnd() = viewModel.requestMoreItems()

	override fun onEmptyActionClick() = viewModel.clearFilter()

	override fun onFilterClick(view: View?) {
		val menu = PopupMenu(view?.context ?: return, view)
		menu.setOnMenuItemClickListener(this)
		val orders = ListSortOrder.FAVORITES.sortedByOrdinal()
		for ((i, item) in orders.withIndex()) {
			menu.menu.add(Menu.NONE, Menu.NONE, i, item.titleResId)
		}
		menu.show()
	}

	override fun onMenuItemClick(item: MenuItem): Boolean {
		val order = ListSortOrder.FAVORITES.sortedByOrdinal().getOrNull(item.order) ?: return false
		viewModel.setSortOrder(order)
		return true
	}

	override fun onCreateActionMode(controller: ListSelectionController, mode: ActionMode, menu: Menu): Boolean {
		mode.menuInflater.inflate(R.menu.mode_favourites, menu)
		return super.onCreateActionMode(controller, mode, menu)
	}

	override fun onPrepareActionMode(controller: ListSelectionController, mode: ActionMode, menu: Menu): Boolean {
		menu.findItem(R.id.action_save)?.isVisible = selectedItems.none { it.isLocal }
		return super.onPrepareActionMode(controller, mode, menu)
	}

	override fun onActionItemClicked(controller: ListSelectionController, mode: ActionMode, item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.action_remove -> {
				viewModel.removeFromFavourites(selectedItemsIds)
				mode.finish()
				true
			}

			R.id.action_mark_current -> {
				MaterialAlertDialogBuilder(context ?: return false)
					.setTitle(item.title)
					.setMessage(R.string.mark_as_completed_prompt)
					.setNegativeButton(android.R.string.cancel, null)
					.setPositiveButton(android.R.string.ok) { _, _ ->
						viewModel.markAsRead(selectedItems)
						mode.finish()
					}.show()
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
