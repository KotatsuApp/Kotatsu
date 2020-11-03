package org.koitharu.kotatsu.ui.settings

import android.os.Bundle
import androidx.preference.MultiSelectListPreference
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.ui.base.BasePreferenceFragment
import org.koitharu.kotatsu.ui.settings.utils.MultiSummaryProvider

class ReaderSettingsFragment : BasePreferenceFragment(R.string.reader_settings) {

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_reader)
		findPreference<MultiSelectListPreference>(AppSettings.KEY_READER_SWITCHERS)?.let {
			it.summaryProvider = MultiSummaryProvider(R.string.gestures_only)
		}
	}
}