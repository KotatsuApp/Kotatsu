package org.koitharu.kotatsu.reader.ui

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.nav.AppRouter

class ReaderMenuProvider(
	private val viewModel: ReaderViewModel,
	private val activity: ReaderActivity,
) : MenuProvider {

	override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
		menuInflater.inflate(R.menu.opt_reader, menu)
	}

	override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
		return when (menuItem.itemId) {
			R.id.action_info -> {
				// TODO
				true
			}
			
			R.id.action_translation_settings -> {
				val manga = viewModel.getMangaOrNull()
				if (manga != null) {
					activity.startActivity(
						AppRouter.translationSettingsIntent(activity, manga)
					)
				}
				true
			}

			else -> false
		}
	}
}
