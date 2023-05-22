package org.koitharu.kotatsu.core.ui.util

import android.view.MenuItem
import android.view.MenuItem.OnActionExpandListener
import androidx.activity.OnBackPressedCallback

class CollapseActionViewCallback(
	private val menuItem: MenuItem
) : OnBackPressedCallback(menuItem.isActionViewExpanded), OnActionExpandListener {

	override fun handleOnBackPressed() {
		menuItem.collapseActionView()
	}

	override fun onMenuItemActionExpand(item: MenuItem): Boolean {
		isEnabled = true
		return true
	}

	override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
		isEnabled = false
		return true
	}
}
