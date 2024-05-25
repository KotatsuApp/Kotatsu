package org.koitharu.kotatsu.settings

import android.content.Context
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import leakcanary.LeakCanary
import org.koitharu.kotatsu.R
import org.koitharu.workinspector.WorkInspector

class SettingsMenuProvider(
	private val context: Context,
) : MenuProvider {

	override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
		menuInflater.inflate(R.menu.opt_settings, menu)
	}

	override fun onMenuItemSelected(menuItem: MenuItem): Boolean = when (menuItem.itemId) {
		R.id.action_leaks -> {
			context.startActivity(LeakCanary.newLeakDisplayActivityIntent())
			true
		}

		R.id.action_works -> {
			context.startActivity(WorkInspector.getIntent(context))
			true
		}

		else -> false
	}
}
