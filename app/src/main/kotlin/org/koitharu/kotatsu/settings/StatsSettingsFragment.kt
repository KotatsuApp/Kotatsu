package org.koitharu.kotatsu.settings

import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.BasePreferenceFragment

@AndroidEntryPoint
class StatsSettingsFragment : BasePreferenceFragment(R.string.reading_stats) {

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_stats)
	}
}
