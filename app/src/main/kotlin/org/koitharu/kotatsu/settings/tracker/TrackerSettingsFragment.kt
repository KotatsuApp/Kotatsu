package org.koitharu.kotatsu.settings.tracker

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.style.URLSpan
import android.view.View
import androidx.core.text.buildSpannedString
import androidx.core.text.inSpans
import androidx.fragment.app.viewModels
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.BasePreferenceFragment
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.settings.tracker.categories.TrackerCategoriesConfigSheet
import org.koitharu.kotatsu.settings.utils.DozeHelper
import org.koitharu.kotatsu.settings.utils.MultiSummaryProvider
import org.koitharu.kotatsu.tracker.work.TrackerNotificationHelper
import javax.inject.Inject

@AndroidEntryPoint
class TrackerSettingsFragment :
	BasePreferenceFragment(R.string.check_for_new_chapters),
	SharedPreferences.OnSharedPreferenceChangeListener {

	private val viewModel by viewModels<TrackerSettingsViewModel>()
	private val dozeHelper = DozeHelper(this)

	@Inject
	lateinit var notificationHelper: TrackerNotificationHelper

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_tracker)

		findPreference<MultiSelectListPreference>(AppSettings.KEY_TRACK_SOURCES)
			?.summaryProvider = MultiSummaryProvider(R.string.dont_check)
		val warningPreference = findPreference<Preference>(AppSettings.KEY_TRACK_WARNING)
		if (warningPreference != null) {
			warningPreference.summary = buildSpannedString {
				append(getString(R.string.tracker_warning))
				append(" ")
				inSpans(URLSpan("https://dontkillmyapp.com/")) {
					append(getString(R.string.read_more))
				}
			}
		}
		dozeHelper.updatePreference()
		updateCategoriesEnabled()
	}

	override fun onResume() {
		super.onResume()
		updateNotificationsSummary()
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		settings.subscribe(this)
		viewModel.categoriesCount.observe(viewLifecycleOwner, ::onCategoriesCountChanged)
	}

	override fun onDestroyView() {
		settings.unsubscribe(this)
		super.onDestroyView()
	}

	override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String?) {
		when (key) {
			AppSettings.KEY_TRACKER_NOTIFICATIONS -> updateNotificationsSummary()
			AppSettings.KEY_TRACK_SOURCES,
			AppSettings.KEY_TRACKER_ENABLED -> updateCategoriesEnabled()
		}
	}

	override fun onPreferenceTreeClick(preference: Preference): Boolean {
		return when (preference.key) {
			AppSettings.KEY_NOTIFICATIONS_SETTINGS -> when {
				Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
					val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
						.putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
					startActivitySafe(intent)
					true
				}

				!notificationHelper.getAreNotificationsEnabled() -> {
					val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
						.setData(Uri.fromParts("package", requireContext().packageName, null))
					startActivitySafe(intent)
					true
				}

				else -> super.onPreferenceTreeClick(preference)
			}

			AppSettings.KEY_TRACK_CATEGORIES -> {
				TrackerCategoriesConfigSheet.show(childFragmentManager)
				true
			}

			AppSettings.KEY_IGNORE_DOZE -> {
				dozeHelper.startIgnoreDoseActivity()
				true
			}

			else -> super.onPreferenceTreeClick(preference)
		}
	}

	private fun updateNotificationsSummary() {
		val pref = findPreference<Preference>(AppSettings.KEY_NOTIFICATIONS_SETTINGS) ?: return
		pref.setSummary(
			when {
				notificationHelper.getAreNotificationsEnabled() -> R.string.show_notification_new_chapters_on
				else -> R.string.show_notification_new_chapters_off
			},
		)
	}

	private fun updateCategoriesEnabled() {
		val pref = findPreference<Preference>(AppSettings.KEY_TRACK_CATEGORIES) ?: return
		pref.isEnabled = settings.isTrackerEnabled && AppSettings.TRACK_FAVOURITES in settings.trackSources
	}

	private fun onCategoriesCountChanged(count: IntArray?) {
		val pref = findPreference<Preference>(AppSettings.KEY_TRACK_CATEGORIES) ?: return
		pref.summary = count?.let {
			getString(R.string.enabled_d_of_d, count[0], count[1])
		}
	}
}
