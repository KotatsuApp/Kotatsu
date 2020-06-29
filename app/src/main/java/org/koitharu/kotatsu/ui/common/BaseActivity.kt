package org.koitharu.kotatsu.ui.common

import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import moxy.MvpAppCompatActivity
import org.koin.core.KoinComponent
import org.koitharu.kotatsu.R

abstract class BaseActivity : MvpAppCompatActivity(), KoinComponent {

	override fun setContentView(layoutResID: Int) {
		super.setContentView(layoutResID)
		setupToolbar()
	}

	override fun setContentView(view: View?) {
		super.setContentView(view)
		setupToolbar()
	}

	private fun setupToolbar() {
		(findViewById<View>(R.id.toolbar) as? Toolbar)?.let(this::setSupportActionBar)
	}

	override fun onOptionsItemSelected(item: MenuItem) = if (item.itemId == android.R.id.home) {
		onBackPressed()
		true
	} else super.onOptionsItemSelected(item)
}