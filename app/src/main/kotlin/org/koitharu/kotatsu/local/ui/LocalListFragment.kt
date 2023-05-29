package org.koitharu.kotatsu.local.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.PopupMenu
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.list.ListSelectionController
import org.koitharu.kotatsu.core.util.ShareHelper
import org.koitharu.kotatsu.core.util.ext.addMenuProvider
import org.koitharu.kotatsu.core.util.ext.observeEvent
import org.koitharu.kotatsu.databinding.FragmentListBinding
import org.koitharu.kotatsu.list.ui.MangaListFragment
import org.koitharu.kotatsu.parsers.model.SortOrder

class LocalListFragment : MangaListFragment(), PopupMenu.OnMenuItemClickListener {

	override val viewModel by viewModels<LocalListViewModel>()

	override fun onViewBindingCreated(binding: FragmentListBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		addMenuProvider(LocalListMenuProvider(this::onEmptyActionClick))
		viewModel.onMangaRemoved.observeEvent(viewLifecycleOwner) { onItemRemoved() }
	}

	override fun onEmptyActionClick() {
		ImportDialogFragment.show(childFragmentManager)
	}

	override fun onFilterClick(view: View?) {
		super.onFilterClick(view)
		val menu = PopupMenu(requireContext(), view ?: requireViewBinding().recyclerView)
		menu.inflate(R.menu.popup_order)
		menu.setOnMenuItemClickListener(this)
		menu.show()
	}

	override fun onScrolledToEnd() = Unit

	override fun onCreateActionMode(controller: ListSelectionController, mode: ActionMode, menu: Menu): Boolean {
		mode.menuInflater.inflate(R.menu.mode_local, menu)
		return super.onCreateActionMode(controller, mode, menu)
	}

	override fun onActionItemClicked(controller: ListSelectionController, mode: ActionMode, item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.action_remove -> {
				showDeletionConfirm(selectedItemsIds, mode)
				true
			}

			R.id.action_share -> {
				val files = selectedItems.map { it.url.toUri().toFile() }
				ShareHelper(requireContext()).shareCbz(files)
				mode.finish()
				true
			}

			else -> super.onActionItemClicked(controller, mode, item)
		}
	}

	override fun onMenuItemClick(item: MenuItem): Boolean {
		val order = when (item.itemId) {
			R.id.action_order_new -> SortOrder.NEWEST
			R.id.action_order_abs -> SortOrder.ALPHABETICAL
			R.id.action_order_rating -> SortOrder.RATING
			else -> return false
		}
		viewModel.setSortOrder(order)
		return true
	}

	private fun showDeletionConfirm(ids: Set<Long>, mode: ActionMode) {
		MaterialAlertDialogBuilder(context ?: return)
			.setTitle(R.string.delete_manga)
			.setMessage(getString(R.string.text_delete_local_manga_batch))
			.setPositiveButton(R.string.delete) { _, _ ->
				viewModel.delete(ids)
				mode.finish()
			}
			.setNegativeButton(android.R.string.cancel, null)
			.show()
	}

	private fun onItemRemoved() {
		Snackbar.make(requireViewBinding().recyclerView, R.string.removal_completed, Snackbar.LENGTH_SHORT).show()
	}

	companion object {

		fun newInstance() = LocalListFragment()
	}
}
