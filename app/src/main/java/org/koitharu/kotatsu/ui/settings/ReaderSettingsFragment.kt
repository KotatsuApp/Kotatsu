package org.koitharu.kotatsu.ui.settings

import android.os.Bundle
import androidx.preference.MultiSelectListPreference
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.ui.common.BasePreferenceFragment
import org.koitharu.kotatsu.ui.settings.utils.MultiSummaryProvider

class ReaderSettingsFragment : BasePreferenceFragment(R.string.reader_settings) {

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_reader)
		findPreference<MultiSelectListPreference>(R.string.key_reader_switchers)?.let {
			it.summaryProvider = MultiSummaryProvider(R.string.gestures_only)
		}
	}
}