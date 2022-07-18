package org.koitharu.kotatsu.history.ui

import android.content.Context
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.koitharu.kotatsu.R
import com.google.android.material.R as materialR

class HistoryListMenuProvider(
	private val context: Context,
	private val viewModel: HistoryListViewModel,
) : MenuProvider {

	override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
		menuInflater.inflate(R.menu.opt_history, menu)
	}

	override fun onMenuItemSelected(menuItem: MenuItem): Boolean = when (menuItem.itemId) {
		R.id.action_clear_history -> {
			MaterialAlertDialogBuilder(context, materialR.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered)
				.setTitle(R.string.clear_history)
				.setMessage(R.string.text_clear_history_prompt)
				.setIcon(R.drawable.ic_delete)
				.setNegativeButton(android.R.string.cancel, null)
				.setPositiveButton(R.string.clear) { _, _ ->
					viewModel.clearHistory()
				}.show()
			true
		}
		R.id.action_history_grouping -> {
			viewModel.setGrouping(!menuItem.isChecked)
			true
		}
		else -> false
	}

	override fun onPrepareMenu(menu: Menu) {
		menu.findItem(R.id.action_history_grouping).isChecked = viewModel.isGroupingEnabled.value == true
	}
}