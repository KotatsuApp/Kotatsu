package org.koitharu.kotatsu.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.preference.Preference
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.BasePreferenceFragment

class RootSettingsFragment : BasePreferenceFragment(0), SharedPreferences.OnSharedPreferenceChangeListener {

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_root)
		bindPreferenceSummary("appearance", R.string.theme, R.string.list_mode, R.string.language)
		bindPreferenceSummary("reader", R.string.read_mode, R.string.scale_mode, R.string.switch_pages)
		bindPreferenceSummary("network", R.string.proxy, R.string.dns_over_https, R.string.prefetch_content)
		bindPreferenceSummary("userdata", R.string.protect_application, R.string.backup_restore, R.string.data_deletion)
		bindPreferenceSummary("downloads", R.string.manga_save_location, R.string.downloads_wifi_only)
		bindPreferenceSummary("tracker", R.string.track_sources, R.string.notifications_settings)
		bindPreferenceSummary("services", R.string.suggestions, R.string.sync, R.string.tracking)
		findPreference<Preference>("about")?.summary = getString(R.string.app_version, BuildConfig.VERSION_NAME)
		bindRemoteSourcesSummary()
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		settings.subscribe(this)
	}

	override fun onDestroyView() {
		settings.unsubscribe(this)
		super.onDestroyView()
	}

	override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
		when (key) {
			AppSettings.KEY_SOURCES_HIDDEN -> {
				bindRemoteSourcesSummary()
			}
		}
	}

	private fun bindPreferenceSummary(key: String, @StringRes vararg items: Int) {
		findPreference<Preference>(key)?.summary = items.joinToString { getString(it) }
	}

	private fun bindRemoteSourcesSummary() {
		findPreference<Preference>(AppSettings.KEY_REMOTE_SOURCES)?.run {
			val total = settings.remoteMangaSources.size
			summary = getString(R.string.enabled_d_of_d, total - settings.hiddenSources.size, total)
		}
	}
}
