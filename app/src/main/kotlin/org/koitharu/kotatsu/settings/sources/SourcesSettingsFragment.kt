package org.koitharu.kotatsu.settings.sources

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.TwoStatePreference
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.nav.router
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.BasePreferenceFragment
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.setDefaultValueCompat
import org.koitharu.kotatsu.explore.data.SourcesSortOrder
import org.koitharu.kotatsu.parsers.util.names

@AndroidEntryPoint
class SourcesSettingsFragment : BasePreferenceFragment(R.string.remote_sources),
	SharedPreferences.OnSharedPreferenceChangeListener {

	private val viewModel by viewModels<SourcesSettingsViewModel>()

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_sources)
		findPreference<ListPreference>(AppSettings.KEY_SOURCES_ORDER)?.run {
			entryValues = SourcesSortOrder.entries.names()
			entries = SourcesSortOrder.entries.map { context.getString(it.titleResId) }.toTypedArray()
			setDefaultValueCompat(SourcesSortOrder.MANUAL.name)
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		findPreference<Preference>(AppSettings.KEY_REMOTE_SOURCES)?.let { pref ->
			viewModel.enabledSourcesCount.observe(viewLifecycleOwner) {
				pref.summary = if (it >= 0) {
					resources.getQuantityString(R.plurals.items, it, it)
				} else {
					null
				}
			}
		}
		findPreference<Preference>(AppSettings.KEY_SOURCES_CATALOG)?.let { pref ->
			viewModel.availableSourcesCount.observe(viewLifecycleOwner) {
				pref.summary = when {
					it == 0 -> getString(R.string.all_sources_enabled)
					it > 0 -> getString(R.string.available_d, it)
					else -> null
				}
			}
		}
		findPreference<TwoStatePreference>(AppSettings.KEY_HANDLE_LINKS)?.let { pref ->
			viewModel.isLinksEnabled.observe(viewLifecycleOwner) {
				pref.isChecked = it
			}
		}
		updateEnableAllDependencies()
		settings.subscribe(this)
	}

	override fun onDestroyView() {
		settings.unsubscribe(this)
		super.onDestroyView()
	}

	override fun onPreferenceTreeClick(preference: Preference): Boolean = when (preference.key) {
		AppSettings.KEY_SOURCES_CATALOG -> {
			router.openSourcesCatalog()
			true
		}

		AppSettings.KEY_HANDLE_LINKS -> {
			viewModel.setLinksEnabled((preference as TwoStatePreference).isChecked)
			true
		}

		else -> super.onPreferenceTreeClick(preference)
	}

	override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
		when (key) {
			AppSettings.KEY_SOURCES_ENABLED_ALL -> updateEnableAllDependencies()
		}
	}

	private fun updateEnableAllDependencies() {
		findPreference<Preference>(AppSettings.KEY_SOURCES_CATALOG)?.isEnabled = !settings.isAllSourcesEnabled
	}
}
