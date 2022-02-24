package org.koitharu.kotatsu.settings.about

import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import kotlinx.coroutines.launch
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BasePreferenceFragment
import org.koitharu.kotatsu.browser.BrowserActivity
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.settings.AppUpdateChecker
import org.koitharu.kotatsu.utils.ext.viewLifecycleScope

class AboutSettingsFragment : BasePreferenceFragment(R.string.about) {

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_about)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		findPreference<Preference>(AppSettings.KEY_APP_UPDATE_AUTO)?.run {
			isVisible = AppUpdateChecker.isUpdateSupported(context)
		}
		findPreference<Preference>(AppSettings.KEY_APP_VERSION)?.run {
			title = getString(R.string.app_version, BuildConfig.VERSION_NAME)
			isEnabled = AppUpdateChecker.isUpdateSupported(context)
		}
	}


	override fun onPreferenceTreeClick(preference: Preference): Boolean {
		return when (preference.key) {
			AppSettings.KEY_APP_VERSION -> {
				checkForUpdates()
				true
			}
			AppSettings.KEY_APP_TRANSLATION -> {
				startActivity(context?.let { BrowserActivity.newIntent(it,
					"https://hosted.weblate.org/engage/kotatsu",
					resources.getString(R.string.about_app_translation)) })
				true
			}
			AppSettings.KEY_FEEDBACK_4PDA -> {
				startActivity(context?.let { BrowserActivity.newIntent(it,
					"https://4pda.to/forum/index.php?showtopic=697669",
					resources.getString(R.string.about_feedback_4pda)) })
				true
			}
			AppSettings.KEY_FEEDBACK_DISCORD -> {
				startActivity(context?.let { BrowserActivity.newIntent(it,
					"https://discord.gg/NNJ5RgVBC5",
					"Discord") })
				true
			}
			AppSettings.KEY_FEEDBACK_GITHUB -> {
				startActivity(context?.let { BrowserActivity.newIntent(it,
					"https://github.com/nv95/Kotatsu/issues",
					"GitHub") })
				true
			}
			AppSettings.KEY_SUPPORT_DEVELOPER -> {
				startActivity(context?.let { BrowserActivity.newIntent(it,
					"https://yoomoney.ru/to/410012543938752",
					resources.getString(R.string.about_support_developer)) })
				true
			}
			AppSettings.KEY_APP_GRATITUDES -> {
				startActivity(context?.let { BrowserActivity.newIntent(it,
					"https://github.com/nv95/Kotatsu/graphs/contributors",
					resources.getString(R.string.about_gratitudes)) })
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
}
