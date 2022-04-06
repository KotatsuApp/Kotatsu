package org.koitharu.kotatsu.settings.about

import android.content.Intent
import android.os.Bundle
import androidx.core.net.toUri
import androidx.preference.Preference
import kotlinx.coroutines.launch
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BasePreferenceFragment
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.settings.AppUpdateChecker
import org.koitharu.kotatsu.utils.ext.viewLifecycleScope

class AboutSettingsFragment : BasePreferenceFragment(R.string.about) {

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_about)
		val isUpdateSupported = AppUpdateChecker.isUpdateSupported(requireContext())
		findPreference<Preference>(AppSettings.KEY_APP_UPDATE_AUTO)?.run {
			isVisible = isUpdateSupported
		}
		findPreference<Preference>(AppSettings.KEY_APP_VERSION)?.run {
			title = getString(R.string.app_version, BuildConfig.VERSION_NAME)
			isEnabled = isUpdateSupported
		}
	}

	override fun onPreferenceTreeClick(preference: Preference): Boolean {
		return when (preference.key) {
			AppSettings.KEY_APP_VERSION -> {
				checkForUpdates()
				true
			}
			AppSettings.KEY_APP_TRANSLATION -> {
				openLink(getString(R.string.url_weblate), preference.title)
				true
			}
			AppSettings.KEY_FEEDBACK_4PDA -> {
				openLink(getString(R.string.url_forpda), preference.title)
				true
			}
			AppSettings.KEY_FEEDBACK_DISCORD -> {
				openLink(getString(R.string.url_discord), preference.title)
				true
			}
			AppSettings.KEY_FEEDBACK_GITHUB -> {
				openLink(getString(R.string.url_github_issues), preference.title)
				true
			}
			else -> super.onPreferenceTreeClick(preference)
		}
	}

	private fun checkForUpdates() {
		viewLifecycleScope.launch {
			findPreference<Preference>(AppSettings.KEY_APP_VERSION)?.run {
				setSummary(R.string.checking_for_updates)
				isSelectable = false
			}
			val result = AppUpdateChecker(activity ?: return@launch).checkNow()
			findPreference<Preference>(AppSettings.KEY_APP_VERSION)?.run {
				setSummary(
					when (result) {
						true -> R.string.check_for_updates
						false -> R.string.no_update_available
						null -> R.string.update_check_failed
					}
				)
				isSelectable = true
			}
		}
	}

	private fun openLink(url: String, title: CharSequence?) {
		val intent = Intent(Intent.ACTION_VIEW)
		intent.data = url.toUri()
		startActivity(
			if (title != null) {
				Intent.createChooser(intent, title)
			} else {
				intent
			}
		)
	}
}