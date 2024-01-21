package org.koitharu.kotatsu.favourites.ui.categories

import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.list.ListSelectionController
import org.koitharu.kotatsu.core.util.ext.DIALOG_THEME_CENTERED

class CategoriesSelectionCallback(
	private val recyclerView: RecyclerView,
	private val viewModel: FavouritesCategoriesViewModel,
) : ListSelectionController.Callback2 {

	override fun onSelectionChanged(controller: ListSelectionController, count: Int) {
		recyclerView.invalidateItemDecorations()
	}

	override fun onCreateActionMode(controller: ListSelectionController, mode: ActionMode, menu: Menu): Boolean {
		mode.menuInflater.inflate(R.menu.mode_category, menu)
		return true
	}

	override fun onPrepareActionMode(controller: ListSelectionController, mode: ActionMode, menu: Menu): Boolean {
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
		mode.title = controller.count.toString()
		return true
	}

	override fun onActionItemClicked(controller: ListSelectionController, mode: ActionMode, item: MenuItem): Boolean {
		return when (item.itemId) {
			/*R.id.action_view -> {
				val id = controller.peekCheckedIds().singleOrNull() ?: return false
				val context = recyclerView.context
				val category = viewModel.getCategory(id) ?: return false
				val intent = FavouritesActivity.newIntent(context, category)
				context.startActivity(intent)
				mode.finish()
				true
			}*/

			R.id.action_show -> {
				viewModel.setIsVisible(controller.snapshot(), true)
				mode.finish()
				true
			}

			R.id.action_hide -> {
				viewModel.setIsVisible(controller.snapshot(), false)
				mode.finish()
				true
			}

			R.id.action_remove -> {
				confirmDeleteCategories(controller.snapshot(), mode)
				true
			}

			else -> false
		}
	}

	private fun confirmDeleteCategories(ids: Set<Long>, mode: ActionMode) {
		val context = recyclerView.context
		MaterialAlertDialogBuilder(context, DIALOG_THEME_CENTERED)
			.setMessage(R.string.categories_delete_confirm)
			.setTitle(R.string.remove_category)
			.setIcon(R.drawable.ic_delete)
			.setNegativeButton(android.R.string.cancel, null)
			.setPositiveButton(R.string.remove) { _, _ ->
				viewModel.deleteCategories(ids)
				mode.finish()
			}.show()
	}
}
