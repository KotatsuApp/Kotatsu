package org.koitharu.kotatsu.tracker.ui.updates

import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.list.ListSelectionController
import org.koitharu.kotatsu.list.ui.MangaListFragment

@AndroidEntryPoint
class UpdatesFragment : MangaListFragment() {

	override val viewModel by viewModels<UpdatesViewModel>()
	override val isSwipeRefreshEnabled = false

	override fun onScrolledToEnd() = Unit

	override fun onCreateActionMode(controller: ListSelectionController, mode: ActionMode, menu: Menu): Boolean {
		mode.menuInflater.inflate(R.menu.mode_updates, menu)
		return super.onCreateActionMode(controller, mode, menu)
	}

	override fun onActionItemClicked(controller: ListSelectionController, mode: ActionMode, item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.action_remove -> {
				viewModel.remove(controller.snapshot())
				mode.finish()
				true
			}

			else -> super.onActionItemClicked(controller, mode, item)
		}
	}

	companion object {

		fun newInstance() = UpdatesFragment()
	}
}
