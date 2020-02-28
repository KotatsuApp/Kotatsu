package org.koitharu.kotatsu.ui.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.collection.arrayMapOf
import androidx.preference.ListPreference
import androidx.preference.Preference
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.ui.common.BasePreferenceFragment
import org.koitharu.kotatsu.ui.main.list.ListModeSelectDialog

class AppearanceSettingsFragment : BasePreferenceFragment(R.string.appearance),
	SharedPreferences.OnSharedPreferenceChangeListener {

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_appearance)

		findPreference<Preference>(R.string.key_list_mode)?.summary =
			listModes[settings.listMode]?.let(::getString)
		findPreference<ListPreference>(R.string.key_theme)?.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		settings.subscribe(this)
	}

	override fun onDestroyView() {
		settings.unsubscribe(this)
		super.onDestroyView()
	}

	override fun onPreferenceTreeClick(preference: Preference?): Boolean {
		return when (preference?.key) {
			getString(R.string.key_list_mode) -> {
				ListModeSelectDialog.show(childFragmentManager)
				true
			}
			else -> super.onPreferenceTreeClick(preference)
		}
	}

	override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
		when (key) {
			getString(R.string.key_list_mode) -> findPreference<Preference>(R.string.key_list_mode)?.summary =
				listModes[settings.listMode]?.let(::getString)
			getString(R.string.key_theme) -> {
				AppCompatDelegate.setDefaultNightMode(settings.theme)
			}
		}
	}

	private companion object {

		val listModes = arrayMapOf(
			ListMode.DETAILED_LIST to R.string.detailed_list,
			ListMode.GRID to R.string.grid,
			ListMode.LIST to R.string.list
		)
	}
}