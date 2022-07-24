package org.koitharu.kotatsu.library.ui

import android.content.Context
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import androidx.fragment.app.FragmentManager
import com.google.android.material.R as materialR
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.*
import java.util.concurrent.TimeUnit
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.dialog.RememberSelectionDialogListener
import org.koitharu.kotatsu.library.ui.config.categories.LibraryCategoriesConfigSheet
import org.koitharu.kotatsu.library.ui.config.size.LibrarySizeBottomSheet
import org.koitharu.kotatsu.utils.ext.startOfDay

class LibraryMenuProvider(
	private val context: Context,
	private val fragmentManager: FragmentManager,
	private val viewModel: LibraryViewModel,
) : MenuProvider {

	override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
		menuInflater.inflate(R.menu.opt_library, menu)
	}

	override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
		return when (menuItem.itemId) {
			R.id.action_clear_history -> {
				showClearHistoryDialog()
				true
			}
			R.id.action_grid_size -> {
				LibrarySizeBottomSheet.show(fragmentManager)
				true
			}
			R.id.action_categories -> {
				LibraryCategoriesConfigSheet.show(fragmentManager)
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
