package org.koitharu.kotatsu.history.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.view.ActionMode
import com.google.android.material.snackbar.Snackbar
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.domain.ReversibleHandle
import org.koitharu.kotatsu.base.domain.reverseAsync
import org.koitharu.kotatsu.list.ui.MangaListFragment
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.utils.ext.addMenuProvider

class HistoryListFragment : MangaListFragment() {

	override val viewModel by viewModel<HistoryListViewModel>()
	override val isSwipeRefreshEnabled = false

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		addMenuProvider(HistoryListMenuProvider(view.context, viewModel))
		viewModel.isGroupingEnabled.observe(viewLifecycleOwner) {
			activity?.invalidateOptionsMenu()
		}
		viewModel.onItemsRemoved.observe(viewLifecycleOwner, ::onItemsRemoved)
	}

	override fun onScrolledToEnd() = Unit

	override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
		mode.menuInflater.inflate(R.menu.mode_history, menu)
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
				viewModel.removeFromHistory(selectedItemsIds)
				mode.finish()
				true
			}
			else -> super.onActionItemClicked(mode, item)
		}
	}

	private fun onItemsRemoved(reversibleHandle: ReversibleHandle) {
		Snackbar.make(binding.recyclerView, R.string.removed_from_history, Snackbar.LENGTH_LONG)
			.setAction(R.string.undo) { reversibleHandle.reverseAsync() }
			.show()
	}

	companion object {

		fun newInstance() = HistoryListFragment()
	}
}