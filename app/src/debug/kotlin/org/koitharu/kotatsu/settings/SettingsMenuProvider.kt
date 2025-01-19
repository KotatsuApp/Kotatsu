package org.koitharu.kotatsu.settings

import android.content.Context
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import leakcanary.LeakCanary
import org.koitharu.kotatsu.KotatsuApp
import org.koitharu.kotatsu.R
import org.koitharu.workinspector.WorkInspector

class SettingsMenuProvider(
	private val context: Context,
) : MenuProvider {

	private val application: KotatsuApp
		get() = context.applicationContext as KotatsuApp

	override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
		menuInflater.inflate(R.menu.opt_settings, menu)
	}

	override fun onPrepareMenu(menu: Menu) {
		super.onPrepareMenu(menu)
		menu.findItem(R.id.action_leakcanary).isChecked = application.isLeakCanaryEnabled
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

		R.id.action_leakcanary -> {
			val checked = !menuItem.isChecked
			menuItem.isChecked = checked
			application.isLeakCanaryEnabled = checked
			true
		}

		else -> false
	}
}
