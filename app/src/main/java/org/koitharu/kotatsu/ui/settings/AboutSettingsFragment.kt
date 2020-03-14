package org.koitharu.kotatsu.ui.settings

import android.os.Bundle
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.ui.common.BasePreferenceFragment

class AboutSettingsFragment : BasePreferenceFragment(R.string.about_app) {

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_about)
	}
}