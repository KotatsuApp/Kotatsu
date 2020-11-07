package org.koitharu.kotatsu.ui.settings

import android.os.Bundle
import android.view.View
import androidx.preference.ListPreference
import androidx.preference.MultiSelectListPreference
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.ZoomMode
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.ui.base.BasePreferenceFragment
import org.koitharu.kotatsu.ui.settings.utils.MultiSummaryProvider
import org.koitharu.kotatsu.utils.ext.names

class ReaderSettingsFragment : BasePreferenceFragment(R.string.reader_settings) {

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_reader)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		findPreference<MultiSelectListPreference>(AppSettings.KEY_READER_SWITCHERS)?.let {
			it.summaryProvider = MultiSummaryProvider(R.string.gestures_only)
		}
		findPreference<ListPreference>(AppSettings.KEY_ZOOM_MODE)?.let {
			it.entryValues = ZoomMode.values().names()
			it.setDefaultValue(ZoomMode.FIT_CENTER.name)
			it.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
		}
	}
}