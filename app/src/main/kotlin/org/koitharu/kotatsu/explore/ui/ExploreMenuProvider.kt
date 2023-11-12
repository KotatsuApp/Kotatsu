package org.koitharu.kotatsu.explore.ui

import android.content.Context
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.settings.SettingsActivity

class ExploreMenuProvider(
	private val context: Context,
	private val viewModel: ExploreViewModel,
) : MenuProvider {

	override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
		menuInflater.inflate(R.menu.opt_explore, menu)
	}

	override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
		return when (menuItem.itemId) {
			R.id.action_grid -> {
				viewModel.setGridMode(!menuItem.isChecked)
				true
			}

			R.id.action_manage -> {
				context.startActivity(SettingsActivity.newManageSourcesIntent(context))
				true
			}

			else -> false
		}
	}

	override fun onPrepareMenu(menu: Menu) {
		super.onPrepareMenu(menu)
		menu.findItem(R.id.action_grid)?.isChecked = viewModel.isGrid.value == true
	}
}
