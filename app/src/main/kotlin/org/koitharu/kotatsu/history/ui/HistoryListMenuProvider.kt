package org.koitharu.kotatsu.history.ui

import android.content.Context
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.nav.AppRouter
import org.koitharu.kotatsu.core.ui.dialog.RememberSelectionDialogListener
import org.koitharu.kotatsu.core.ui.dialog.buildAlertDialog
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class HistoryListMenuProvider(
	private val context: Context,
	private val router: AppRouter,
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
				router.openStatistic()
				true
			}

			else -> false
		}
	}

	private fun showClearHistoryDialog() {
		val selectionListener = RememberSelectionDialogListener(1)
		buildAlertDialog(context, isCentered = true) {
			setTitle(R.string.clear_history)
			setSingleChoiceItems(
				arrayOf(
					context.getString(R.string.last_2_hours),
					context.getString(R.string.today),
					context.getString(R.string.not_in_favorites),
					context.getString(R.string.clear_all_history),
				),
				selectionListener.selection,
				selectionListener,
			)
			setIcon(R.drawable.ic_delete_all)
			setNegativeButton(android.R.string.cancel, null)
			setPositiveButton(R.string.clear) { _, _ ->
				when (selectionListener.selection) {
					0 -> viewModel.clearHistory(Instant.now().minus(2, ChronoUnit.HOURS))
					1 -> viewModel.clearHistory(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant())
					2 -> viewModel.removeNotFavorite()
					3 -> viewModel.clearHistory(null)
				}
			}
		}.show()
	}
}
