package org.koitharu.kotatsu.settings

import android.os.Bundle
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.BasePreferenceFragment

class RootSettingsFragment : BasePreferenceFragment(0) {

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_root)
	}
}
