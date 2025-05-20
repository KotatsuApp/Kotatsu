package org.koitharu.kotatsu.favourites.ui.categories

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import androidx.recyclerview.widget.RecyclerView
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.dialog.buildAlertDialog
import org.koitharu.kotatsu.core.ui.list.ListSelectionController

class CategoriesSelectionCallback(
	private val recyclerView: RecyclerView,
	private val viewModel: FavouritesCategoriesViewModel,
) : ListSelectionController.Callback {

	override fun onSelectionChanged(controller: ListSelectionController, count: Int) {
		recyclerView.invalidateItemDecorations()
	}

	override fun onCreateActionMode(
		controller: ListSelectionController,
		menuInflater: MenuInflater,
		menu: Menu
	): Boolean {
		menuInflater.inflate(R.menu.mode_category, menu)
		return true
	}

	override fun onPrepareActionMode(controller: ListSelectionController, mode: ActionMode?, menu: Menu): Boolean {
		val categories = viewModel.getCategories(controller.peekCheckedIds())
		var canShow = categories.isNotEmpty()
		var canHide = canShow
		for (cat in categories) {
			if (cat.isVisibleInLibrary) {
				canShow = false
			} else {
				canHide = false
			}
		}
		menu.findItem(R.id.action_show)?.isVisible = canShow
		menu.findItem(R.id.action_hide)?.isVisible = canHide
		mode?.title = controller.count.toString()
		return true
	}

	override fun onActionItemClicked(controller: ListSelectionController, mode: ActionMode?, item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.action_show -> {
				viewModel.setIsVisible(controller.snapshot(), true)
				mode?.finish()
				true
			}

			R.id.action_hide -> {
				viewModel.setIsVisible(controller.snapshot(), false)
				mode?.finish()
				true
			}

			R.id.action_remove -> {
				confirmDeleteCategories(controller.snapshot(), mode)
				true
			}

			else -> false
		}
	}

	private fun confirmDeleteCategories(ids: Set<Long>, mode: ActionMode?) {
		buildAlertDialog(recyclerView.context, isCentered = true) {
			setMessage(R.string.categories_delete_confirm)
			setTitle(R.string.remove_category)
			setIcon(R.drawable.ic_delete)
			setNegativeButton(android.R.string.cancel, null)
			setPositiveButton(R.string.remove) { _, _ ->
				viewModel.deleteCategories(ids)
				mode?.finish()
			}
		}.show()
	}
}
