package org.koitharu.kotatsu.history.ui

import android.content.Context
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import androidx.core.view.forEach
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.dialog.RememberSelectionDialogListener
import org.koitharu.kotatsu.core.util.ext.startOfDay
import org.koitharu.kotatsu.history.domain.model.HistoryOrder
import java.util.Date
import java.util.concurrent.TimeUnit
import com.google.android.material.R as materialR

class HistoryListMenuProvider(
	private val context: Context,
	private val viewModel: HistoryListViewModel,
) : MenuProvider {

	override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
		menuInflater.inflate(R.menu.opt_history, menu)
		val subMenu = menu.findItem(R.id.action_order)?.subMenu ?: return
		for (order in HistoryOrder.entries) {
			subMenu.add(R.id.group_order, Menu.NONE, order.ordinal, order.titleResId)
		}
		subMenu.setGroupCheckable(R.id.group_order, true, true)
	}

	override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
		if (menuItem.groupId == R.id.group_order) {
			val order = HistoryOrder.entries[menuItem.order]
			viewModel.setSortOrder(order)
			return true
		}
		return when (menuItem.itemId) {
			R.id.action_clear_history -> {
				showClearHistoryDialog()
				true
			}

			R.id.action_history_grouping -> {
				viewModel.setGrouping(!menuItem.isChecked)
				true
			}

			else -> false
		}
	}

	override fun onPrepareMenu(menu: Menu) {
		val order = viewModel.sortOrder.value
		menu.findItem(R.id.action_order)?.subMenu?.forEach { item ->
			if (item.order == order.ordinal) {
				item.isChecked = true
			}
		}
		menu.findItem(R.id.action_history_grouping)?.run {
			isChecked = viewModel.isGroupingEnabled.value == true
			isEnabled = order.isGroupingSupported()
		}
	}

	private fun showClearHistoryDialog() {
		val selectionListener = RememberSelectionDialogListener(2)
		MaterialAlertDialogBuilder(context, materialR.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered)
			.setTitle(R.string.clear_history)
			.setSingleChoiceItems(
				arrayOf(
					context.getString(R.string.last_2_hours),
					context.getString(R.string.today),
					context.getString(R.string.clear_all_history),
				),
				selectionListener.selection,
				selectionListener,
			)
			.setIcon(R.drawable.ic_delete)
			.setNegativeButton(android.R.string.cancel, null)
			.setPositiveButton(R.string.clear) { _, _ ->
				val minDate = when (selectionListener.selection) {
					0 -> System.currentTimeMillis() - TimeUnit.HOURS.toMillis(2)
					1 -> Date().startOfDay()
					2 -> 0L
					else -> return@setPositiveButton
				}
				viewModel.clearHistory(minDate)
			}.show()
	}
}
