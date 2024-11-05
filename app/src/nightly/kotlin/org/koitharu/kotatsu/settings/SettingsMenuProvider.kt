package org.koitharu.kotatsu.settings

import android.content.Context
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider

@Suppress("UNUSED_PARAMETER")
class SettingsMenuProvider(context: Context) : MenuProvider {

	override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) = Unit

	override fun onMenuItemSelected(menuItem: MenuItem): Boolean = false
}
