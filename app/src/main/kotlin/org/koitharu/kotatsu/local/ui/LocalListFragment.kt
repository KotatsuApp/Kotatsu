package org.koitharu.kotatsu.local.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.view.ActionMode
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
import org.koitharu.kotatsu.core.util.ext.withArgs
import org.koitharu.kotatsu.databinding.FragmentListBinding
import org.koitharu.kotatsu.filter.ui.FilterOwner
import org.koitharu.kotatsu.filter.ui.MangaFilter
import org.koitharu.kotatsu.filter.ui.sheet.FilterSheetFragment
import org.koitharu.kotatsu.list.ui.MangaListFragment
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.remotelist.ui.RemoteListFragment

class LocalListFragment : MangaListFragment(), FilterOwner {

	init {
		withArgs(1) {
			putSerializable(
				RemoteListFragment.ARG_SOURCE,
				MangaSource.LOCAL,
			) // required by FilterCoordinator
		}
	}

	override val viewModel by viewModels<LocalListViewModel>()

	override val filter: MangaFilter
		get() = viewModel

	override fun onViewBindingCreated(binding: FragmentListBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		addMenuProvider(LocalListMenuProvider(binding.root.context, this::onEmptyActionClick))
		viewModel.onMangaRemoved.observeEvent(viewLifecycleOwner) { onItemRemoved() }
	}

	override fun onEmptyActionClick() {
		ImportDialogFragment.show(childFragmentManager)
	}

	override fun onFilterClick(view: View?) {
		FilterSheetFragment.show(childFragmentManager)
	}

	override fun onScrolledToEnd() = viewModel.loadNextPage()

	override fun onCreateActionMode(
		controller: ListSelectionController,
		mode: ActionMode,
		menu: Menu,
	): Boolean {
		mode.menuInflater.inflate(R.menu.mode_local, menu)
		return super.onCreateActionMode(controller, mode, menu)
	}

	override fun onActionItemClicked(
		controller: ListSelectionController,
		mode: ActionMode,
		item: MenuItem,
	): Boolean {
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
		Snackbar.make(
			requireViewBinding().recyclerView,
			R.string.removal_completed,
			Snackbar.LENGTH_SHORT,
		).show()
	}
}
