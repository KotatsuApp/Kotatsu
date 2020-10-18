package org.koitharu.kotatsu.ui.reader

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.commit
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.ui.base.BaseActivity
import org.koitharu.kotatsu.ui.settings.MainSettingsFragment
import org.koitharu.kotatsu.ui.settings.NetworkSettingsFragment
import org.koitharu.kotatsu.ui.settings.ReaderSettingsFragment

class SimpleSettingsActivity : BaseActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_settings_simple)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		supportFragmentManager.commit {
			replace(
				R.id.container, when (intent?.action) {
					Intent.ACTION_MANAGE_NETWORK_USAGE -> NetworkSettingsFragment()
					ACTION_READER -> ReaderSettingsFragment()
					else -> MainSettingsFragment()
				}
			)
		}
	}

	companion object {

		private const val ACTION_READER =
			"${BuildConfig.APPLICATION_ID}.action.MANAGE_READER_SETTINGS"

		fun newReaderSettingsIntent(context: Context) =
			Intent(context, SimpleSettingsActivity::class.java)
				.setAction(ACTION_READER)
	}
}