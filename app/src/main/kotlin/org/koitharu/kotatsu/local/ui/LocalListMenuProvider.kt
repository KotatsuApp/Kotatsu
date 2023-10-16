package org.koitharu.kotatsu.local.ui

import android.content.Context
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.settings.storage.directories.MangaDirectoriesActivity

class LocalListMenuProvider(
	private val context: Context,
	private val onImportClick: Function0<Unit>,
) : MenuProvider {

	override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
		menuInflater.inflate(R.menu.opt_local, menu)
	}

	override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
		return when (menuItem.itemId) {
			R.id.action_import -> {
				onImportClick()
				true
			}

			R.id.action_directories -> {
				context.startActivity(MangaDirectoriesActivity.newIntent(context))
				true
			}

			else -> false
		}
	}
}
