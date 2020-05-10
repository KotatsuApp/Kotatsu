package org.koitharu.kotatsu.ui.common

import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import moxy.MvpAppCompatActivity
import org.koin.core.KoinComponent
import org.koitharu.kotatsu.BuildConfig
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

	override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
		//TODO remove. Just for testing
		if (BuildConfig.DEBUG && keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
			recreate()
			return true
		}
		return super.onKeyDown(keyCode, event)
	}
}