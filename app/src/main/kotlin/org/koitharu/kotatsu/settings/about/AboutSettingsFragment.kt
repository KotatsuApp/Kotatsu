package org.koitharu.kotatsu.settings.about

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.net.toUri
import androidx.fragment.app.viewModels
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.github.AppVersion
import org.koitharu.kotatsu.core.github.VersionId
import org.koitharu.kotatsu.core.github.isStable
import org.koitharu.kotatsu.core.logs.FileLogger
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.BasePreferenceFragment
import org.koitharu.kotatsu.core.util.ShareHelper
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.observeEvent
import org.koitharu.kotatsu.settings.SettingsActivity
import javax.inject.Inject

@AndroidEntryPoint
class AboutSettingsFragment : BasePreferenceFragment(R.string.about) {

	private val viewModel by viewModels<AboutSettingsViewModel>()

	@Inject
	lateinit var loggers: Set<@JvmSuppressWildcards FileLogger>

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_about)
		findPreference<Preference>(AppSettings.KEY_APP_VERSION)?.run {
			title = getString(R.string.app_version, BuildConfig.VERSION_NAME)
			isEnabled = viewModel.isUpdateSupported
		}
		findPreference<SwitchPreferenceCompat>(AppSettings.KEY_UPDATES_UNSTABLE)?.run {
			isEnabled = VersionId(BuildConfig.VERSION_NAME).isStable
			if (!isEnabled) isChecked = true
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		viewModel.isLoading.observe(viewLifecycleOwner) {
			findPreference<Preference>(AppSettings.KEY_APP_UPDATE)?.isEnabled = !it
		}
		viewModel.onUpdateAvailable.observeEvent(viewLifecycleOwner, ::onUpdateAvailable)
	}

	override fun onPreferenceTreeClick(preference: Preference): Boolean {
		return when (preference.key) {
			AppSettings.KEY_APP_VERSION -> {
				viewModel.checkForUpdates()
				true
			}

			AppSettings.KEY_APP_TRANSLATION -> {
				openLink(getString(R.string.url_weblate), preference.title)
				true
			}

			AppSettings.KEY_LOGS_SHARE -> {
				ShareHelper(preference.context).shareLogs(loggers)
				true
			}

			else -> super.onPreferenceTreeClick(preference)
		}
	}

	private fun onUpdateAvailable(version: AppVersion?) {
		if (version == null) {
			Snackbar.make(listView, R.string.no_update_available, Snackbar.LENGTH_SHORT).show()
			return
		}
		(activity as SettingsActivity).appUpdateDialog.show(version)
	}

	private fun openLink(url: String, title: CharSequence?) {
		val intent = Intent(Intent.ACTION_VIEW)
		intent.data = url.toUri()
		startActivitySafe(
			if (title != null) {
				Intent.createChooser(intent, title)
			} else {
				intent
			},
		)
	}
}
