package org.koitharu.kotatsu.favourites.ui.categories

import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.list.ListSelectionController
import org.koitharu.kotatsu.favourites.ui.categories.edit.FavouritesCategoryEditActivity
import com.google.android.material.R as materialR

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
		val isOneItem = controller.count == 1
		menu.findItem(R.id.action_edit)?.isVisible = isOneItem
		mode.title = controller.count.toString()
		return true
	}

	override fun onActionItemClicked(controller: ListSelectionController, mode: ActionMode, item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.action_edit -> {
				val id = controller.peekCheckedIds().singleOrNull() ?: return false
				val context = recyclerView.context
				val intent = FavouritesCategoryEditActivity.newIntent(context, id)
				context.startActivity(intent)
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
		MaterialAlertDialogBuilder(context, materialR.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered)
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