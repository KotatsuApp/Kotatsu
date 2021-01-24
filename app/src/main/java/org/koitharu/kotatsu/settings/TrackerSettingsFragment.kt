package org.koitharu.kotatsu.settings

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import com.google.android.material.snackbar.Snackbar
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BasePreferenceFragment
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.settings.utils.MultiSummaryProvider
import org.koitharu.kotatsu.tracker.work.TrackWorker

class TrackerSettingsFragment : BasePreferenceFragment(R.string.new_chapters_checking) {

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_tracker)

		findPreference<MultiSelectListPreference>(AppSettings.KEY_TRACK_SOURCES)
			?.summaryProvider = MultiSummaryProvider(R.string.dont_check)
		findPreference<Preference>(AppSettings.KEY_DOZE_WHITELIST)
			?.isVisible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
	}

	override fun onPreferenceTreeClick(preference: Preference?): Boolean {
		return when (preference?.key) {
			AppSettings.KEY_NOTIFICATIONS_SETTINGS -> {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
					val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
						.putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
						.putExtra(Settings.EXTRA_CHANNEL_ID, TrackWorker.CHANNEL_ID)
					startActivity(intent)
				} else {
					(activity as? SettingsActivity)?.openNotificationSettingsLegacy()
				}
				true
			}
			AppSettings.KEY_DOZE_WHITELIST -> {
				disablePowerOptimization()
				true
			}
			else -> super.onPreferenceTreeClick(preference)
		}
	}

	@SuppressLint("BatteryLife")
	private fun disablePowerOptimization() {
		val context = context ?: return
		val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
		if (powerManager == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
			Snackbar.make(
				listView ?: return,
				R.string.operation_not_supported,
				Snackbar.LENGTH_LONG
			).show()
			return
		}
		val packageName = context.packageName
		if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
			val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
				.setData(Uri.parse("package:$packageName"))
			try {
				startActivity(intent)
			} catch (e: ActivityNotFoundException) {
				Snackbar.make(
					listView ?: return,
					R.string.operation_not_supported,
					Snackbar.LENGTH_LONG
				).show()
			}
		} else {
			Snackbar.make(
				listView ?: return,
				R.string.power_optimization_already_disabled,
				Snackbar.LENGTH_LONG
			).show()
		}
	}
}