package org.koitharu.kotatsu.base.ui

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import org.koin.android.ext.android.get
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.prefs.AppSettings

abstract class BaseActivity : AppCompatActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		if (get<AppSettings>().isAmoledTheme) {
			setTheme(R.style.AppTheme_Amoled)
		}
		super.onCreate(savedInstanceState)
	}

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