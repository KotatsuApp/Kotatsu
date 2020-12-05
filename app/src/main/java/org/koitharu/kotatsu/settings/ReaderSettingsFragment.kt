package org.koitharu.kotatsu.settings

import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.MultiSelectListPreference
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BasePreferenceFragment
import org.koitharu.kotatsu.core.model.ZoomMode
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.settings.utils.MultiSummaryProvider
import org.koitharu.kotatsu.utils.ext.names
import org.koitharu.kotatsu.utils.ext.setDefaultValueCompat

class ReaderSettingsFragment : BasePreferenceFragment(R.string.reader_settings) {

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_reader)
		findPreference<MultiSelectListPreference>(AppSettings.KEY_READER_SWITCHERS)?.let {
			it.summaryProvider = MultiSummaryProvider(R.string.gestures_only)
		}
		findPreference<ListPreference>(AppSettings.KEY_ZOOM_MODE)?.let {
			it.entryValues = ZoomMode.values().names()
			it.setDefaultValueCompat(ZoomMode.FIT_CENTER.name)
		}
	}
}