package org.koitharu.kotatsu.core.ui.util

import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.MenuProvider
import org.koitharu.kotatsu.core.util.ext.setOnContextClickListenerCompat

class PopupMenuMediator(
	private val provider: MenuProvider,
) : View.OnLongClickListener, PopupMenu.OnMenuItemClickListener, PopupMenu.OnDismissListener {

	override fun onLongClick(v: View): Boolean {
		val menu = PopupMenu(v.context, v)
		provider.onCreateMenu(menu.menu, menu.menuInflater)
		provider.onPrepareMenu(menu.menu)
		if (!menu.menu.hasVisibleItems()) {
			return false
		}
		menu.setOnMenuItemClickListener(this)
		menu.setOnDismissListener(this)
		menu.show()
		return true
	}

	override fun onMenuItemClick(item: MenuItem): Boolean {
		return provider.onMenuItemSelected(item)
	}

	override fun onDismiss(menu: PopupMenu) {
		provider.onMenuClosed(menu.menu)
	}

	fun attach(view: View) {
		view.setOnLongClickListener(this)
		view.setOnContextClickListenerCompat(this)
	}
}
