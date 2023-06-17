package org.koitharu.kotatsu.settings.tracker

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.style.URLSpan
import android.view.View
import androidx.core.net.toUri
import androidx.core.text.buildSpannedString
import androidx.core.text.inSpans
import androidx.fragment.app.viewModels
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.BasePreferenceFragment
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.powerManager
import org.koitharu.kotatsu.settings.tracker.categories.TrackerCategoriesConfigSheet
import org.koitharu.kotatsu.settings.utils.MultiSummaryProvider
import org.koitharu.kotatsu.tracker.work.TrackerNotificationChannels
import javax.inject.Inject

private const val KEY_IGNORE_DOZE = "ignore_dose"

@AndroidEntryPoint
class TrackerSettingsFragment :
	BasePreferenceFragment(R.string.check_for_new_chapters),
	SharedPreferences.OnSharedPreferenceChangeListener {

	private val viewModel by viewModels<TrackerSettingsViewModel>()

	@Inject
	lateinit var channels: TrackerNotificationChannels

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
		updateDozePreference()
		updateCategoriesEnabled()
	}

	override fun onResume() {
		super.onResume()
		updateDozePreference()
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
					startActivity(intent)
					true
				}

				channels.areNotificationsDisabled -> {
					val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
						.setData(Uri.fromParts("package", requireContext().packageName, null))
					startActivity(intent)
					true
				}

				else -> super.onPreferenceTreeClick(preference)
			}

			AppSettings.KEY_TRACK_CATEGORIES -> {
				TrackerCategoriesConfigSheet.show(childFragmentManager)
				true
			}

			KEY_IGNORE_DOZE -> {
				startIgnoreDoseActivity(preference.context)
				true
			}

			else -> super.onPreferenceTreeClick(preference)
		}
	}

	private fun updateDozePreference() {
		findPreference<Preference>(KEY_IGNORE_DOZE)?.run {
			isVisible = isDozeIgnoreAvailable(context)
		}
	}

	private fun updateNotificationsSummary() {
		val pref = findPreference<Preference>(AppSettings.KEY_NOTIFICATIONS_SETTINGS) ?: return
		pref.setSummary(
			when {
				channels.areNotificationsDisabled -> R.string.disabled
				channels.isNotificationGroupEnabled() -> R.string.show_notification_new_chapters_on
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

	@SuppressLint("BatteryLife")
	private fun startIgnoreDoseActivity(context: Context) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
			Snackbar.make(listView, R.string.operation_not_supported, Snackbar.LENGTH_SHORT).show()
			return
		}
		val packageName = context.packageName
		val powerManager = context.powerManager ?: return
		if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
			try {
				val intent = Intent(
					Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
					"package:$packageName".toUri(),
				)
				startActivity(intent)
			} catch (e: ActivityNotFoundException) {
				Snackbar.make(listView, R.string.operation_not_supported, Snackbar.LENGTH_SHORT).show()
			}
		}
	}

	private fun isDozeIgnoreAvailable(context: Context): Boolean {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
			return false
		}
		val packageName = context.packageName
		val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
		return !powerManager.isIgnoringBatteryOptimizations(packageName)
	}
}
