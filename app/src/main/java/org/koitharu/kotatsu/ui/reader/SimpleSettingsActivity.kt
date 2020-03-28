package org.koitharu.kotatsu.ui.reader

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.commit
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.ui.common.BaseActivity
import org.koitharu.kotatsu.ui.settings.MainSettingsFragment
import org.koitharu.kotatsu.ui.settings.ReaderSettingsFragment

class SimpleSettingsActivity : BaseActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_settings_simple)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		val section = intent?.getIntExtra(EXTRA_SECTION, 0)
		supportFragmentManager.commit {
			replace(R.id.container, when(section) {
				SECTION_READER -> ReaderSettingsFragment()
				else -> MainSettingsFragment()
			})
		}
	}

	companion object {

		private const val EXTRA_SECTION = "section"
		private const val SECTION_READER = 1

		fun newReaderSettingsIntent(context: Context) = Intent(context, SimpleSettingsActivity::class.java)
			.putExtra(EXTRA_SECTION, SECTION_READER)
	}
}