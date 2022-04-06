package org.koitharu.kotatsu.settings

import android.os.Bundle
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BasePreferenceFragment

class RootSettingsFragment : BasePreferenceFragment(R.string.settings) {

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_root)
	}
}