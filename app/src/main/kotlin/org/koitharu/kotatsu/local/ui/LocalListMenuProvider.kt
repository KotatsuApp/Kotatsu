package org.koitharu.kotatsu.local.ui

import android.content.Intent
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.filter.ui.sheet.FilterSheetFragment
import org.koitharu.kotatsu.settings.storage.directories.MangaDirectoriesActivity

class LocalListMenuProvider(
	private val fragment: Fragment,
	private val onImportClick: Function0<Unit>,
) : MenuProvider {

	override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
		menuInflater.inflate(R.menu.opt_local, menu)
	}

	override fun onPrepareMenu(menu: Menu) {
		super.onPrepareMenu(menu)
		menu.findItem(R.id.action_filter)?.isVisible = FilterSheetFragment.isSupported(fragment)
	}

	override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
		return when (menuItem.itemId) {
			R.id.action_import -> {
				onImportClick()
				true
			}

			R.id.action_directories -> {
				fragment.context?.run {
					startActivity(Intent(this, MangaDirectoriesActivity::class.java))
				}
				true
			}

			R.id.action_filter -> {
				FilterSheetFragment.show(fragment.childFragmentManager)
				true
			}

			else -> false
		}
	}
}
