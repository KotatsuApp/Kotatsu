package org.koitharu.kotatsu.settings

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
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BasePreferenceFragment
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.favourites.ui.categories.CategoriesActivity
import org.koitharu.kotatsu.settings.utils.MultiSummaryProvider
import org.koitharu.kotatsu.tracker.domain.TrackingRepository
import org.koitharu.kotatsu.tracker.work.TrackerNotificationChannels
import org.koitharu.kotatsu.utils.ext.viewLifecycleScope

class TrackerSettingsFragment :
	BasePreferenceFragment(R.string.check_for_new_chapters),
	SharedPreferences.OnSharedPreferenceChangeListener {

	private val repository by inject<TrackingRepository>()
	private val channels by inject<TrackerNotificationChannels>()

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
		updateCategoriesEnabled()
	}

	override fun onResume() {
		super.onResume()
		updateCategoriesSummary()
		updateNotificationsSummary()
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		settings.subscribe(this)
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
					startActivity(intent)
					true
				}
				channels.areNotificationsDisabled -> {
					val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
						.setData(Uri.fromParts("package", requireContext().packageName, null))
					startActivity(intent)
					true
				}
				else -> {
					super.onPreferenceTreeClick(preference)
				}
			}
			AppSettings.KEY_TRACK_CATEGORIES -> {
				startActivity(CategoriesActivity.newIntent(preference.context))
				true
			}
			else -> super.onPreferenceTreeClick(preference)
		}
	}

	private fun updateNotificationsSummary() {
		val pref = findPreference<Preference>(AppSettings.KEY_NOTIFICATIONS_SETTINGS) ?: return
		pref.setSummary(
			when {
				channels.areNotificationsDisabled -> R.string.disabled
				channels.isNotificationGroupEnabled() -> R.string.show_notification_new_chapters_on
				else -> R.string.show_notification_new_chapters_off
			}
		)
	}

	private fun updateCategoriesEnabled() {
		val pref = findPreference<Preference>(AppSettings.KEY_TRACK_CATEGORIES) ?: return
		pref.isEnabled = settings.isTrackerEnabled && AppSettings.TRACK_FAVOURITES in settings.trackSources
	}

	private fun updateCategoriesSummary() {
		val pref = findPreference<Preference>(AppSettings.KEY_TRACK_CATEGORIES) ?: return
		viewLifecycleScope.launch {
			val count = repository.getCategoriesCount()
			pref.summary = getString(R.string.enabled_d_of_d, count[0], count[1])
		}
	}
}