package org.koitharu.kotatsu.search.ui.suggestion

import android.content.Context
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.activity.result.ActivityResultLauncher
import androidx.core.view.MenuProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.util.ext.DIALOG_THEME_CENTERED
import org.koitharu.kotatsu.core.util.ext.resolve
import org.koitharu.kotatsu.core.util.ext.tryLaunch
import com.google.android.material.R as materialR

class SearchSuggestionMenuProvider(
	private val context: Context,
	private val voiceInputLauncher: ActivityResultLauncher<String?>,
	private val viewModel: SearchSuggestionViewModel,
) : MenuProvider {

	override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
		menuInflater.inflate(R.menu.opt_search_suggestion, menu)
	}

	override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
		return when (menuItem.itemId) {
			R.id.action_clear -> {
				clearSearchHistory()
				true
			}

			R.id.action_voice_search -> {
				voiceInputLauncher.tryLaunch(context.getString(R.string.search_manga), null)
			}

			else -> false
		}
	}

	override fun onPrepareMenu(menu: Menu) {
		super.onPrepareMenu(menu)
		menu.findItem(R.id.action_voice_search)?.isVisible = voiceInputLauncher.resolve(context, null) != null
	}

	private fun clearSearchHistory() {
		MaterialAlertDialogBuilder(context, DIALOG_THEME_CENTERED)
			.setTitle(R.string.clear_search_history)
			.setIcon(R.drawable.ic_clear_all)
			.setMessage(R.string.text_clear_search_history_prompt)
			.setNegativeButton(android.R.string.cancel, null)
			.setPositiveButton(R.string.clear) { _, _ ->
				viewModel.clearSearchHistory()
			}.show()
	}
}
