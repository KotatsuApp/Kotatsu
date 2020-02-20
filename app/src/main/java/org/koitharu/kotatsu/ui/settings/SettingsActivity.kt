package org.koitharu.kotatsu.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.ui.common.BaseActivity

class SettingsActivity : BaseActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_settings)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)

		if (supportFragmentManager.findFragmentById(R.id.container) == null) {
			supportFragmentManager.beginTransaction()
				.replace(R.id.container, SettingsHeadersFragment())
				.commit()
		}
	}

	companion object {

		fun newIntent(context: Context) = Intent(context, SettingsActivity::class.java)
	}
}