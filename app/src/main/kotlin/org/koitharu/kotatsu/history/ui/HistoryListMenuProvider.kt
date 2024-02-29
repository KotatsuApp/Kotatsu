package org.koitharu.kotatsu.history.ui

import android.content.Context
import android.content.Intent
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.dialog.RememberSelectionDialogListener
import org.koitharu.kotatsu.core.util.ext.DIALOG_THEME_CENTERED
import org.koitharu.kotatsu.stats.ui.StatsActivity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import com.google.android.material.R as materialR

class HistoryListMenuProvider(
	private val context: Context,
	private val viewModel: HistoryListViewModel,
) : MenuProvider {

	override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
		menuInflater.inflate(R.menu.opt_history, menu)
	}

	override fun onPrepareMenu(menu: Menu) {
		super.onPrepareMenu(menu)
		menu.findItem(R.id.action_stats)?.isVisible = viewModel.isStatsEnabled.value
	}

	override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
		return when (menuItem.itemId) {
			R.id.action_clear_history -> {
				showClearHistoryDialog()
				true
			}

			R.id.action_stats -> {
				context.startActivity(Intent(context, StatsActivity::class.java))
				true
			}

			else -> false
		}
	}

	private fun showClearHistoryDialog() {
		val selectionListener = RememberSelectionDialogListener(2)
		MaterialAlertDialogBuilder(context, DIALOG_THEME_CENTERED)
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
					0 -> Instant.now().minus(2, ChronoUnit.HOURS)
					1 -> LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
					2 -> Instant.EPOCH
					else -> return@setPositiveButton
				}
				viewModel.clearHistory(minDate)
			}.show()
	}
}
