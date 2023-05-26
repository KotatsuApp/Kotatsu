package org.koitharu.kotatsu.shelf.ui

import android.content.Context
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import androidx.fragment.app.FragmentManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.dialog.RememberSelectionDialogListener
import org.koitharu.kotatsu.core.util.ext.startOfDay
import org.koitharu.kotatsu.local.ui.ImportDialogFragment
import org.koitharu.kotatsu.shelf.ui.config.ShelfSettingsActivity
import org.koitharu.kotatsu.shelf.ui.config.size.ShelfSizeBottomSheet
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import com.google.android.material.R as materialR

class ShelfMenuProvider(
	private val context: Context,
	private val fragmentManager: FragmentManager,
	private val viewModel: ShelfViewModel,
) : MenuProvider {

	override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
		menuInflater.inflate(R.menu.opt_shelf, menu)
	}

	override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
		return when (menuItem.itemId) {
			R.id.action_clear_history -> {
				showClearHistoryDialog()
				true
			}

			R.id.action_grid_size -> {
				ShelfSizeBottomSheet.show(fragmentManager)
				true
			}

			R.id.action_import -> {
				ImportDialogFragment.show(fragmentManager)
				true
			}

			R.id.action_categories -> {
				context.startActivity(ShelfSettingsActivity.newIntent(context))
				true
			}

			else -> false
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
				val minInstant = when (selectionListener.selection) {
					0 -> Instant.now().minus(2, ChronoUnit.HOURS)
					1 -> LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
					2 -> Instant.EPOCH
					else -> return@setPositiveButton
				}
				viewModel.clearHistory(minInstant.toEpochMilli())
			}.show()
	}
}
