package org.koitharu.kotatsu.settings

import android.content.SharedPreferences
import android.media.RingtoneManager
import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.BasePreferenceFragment
import org.koitharu.kotatsu.settings.utils.RingtonePickContract

class NotificationSettingsLegacyFragment :
	BasePreferenceFragment(R.string.notifications),
	SharedPreferences.OnSharedPreferenceChangeListener {

	private val ringtonePickContract = registerForActivityResult(
		RingtonePickContract(R.string.notification_sound),
	) { uri ->
		settings.notificationSound = uri ?: return@registerForActivityResult
		findPreference<Preference>(AppSettings.KEY_NOTIFICATIONS_SOUND)?.run {
			summary = RingtoneManager.getRingtone(context, uri)?.getTitle(context)
				?: getString(R.string.silent)
		}
	}

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_notifications)
		findPreference<Preference>(AppSettings.KEY_NOTIFICATIONS_SOUND)?.run {
			val uri = settings.notificationSound
			summary = RingtoneManager.getRingtone(context, uri)?.getTitle(context)
				?: getString(R.string.silent)
		}
		updateInfo()
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		settings.subscribe(this)
	}

	override fun onDestroyView() {
		settings.unsubscribe(this)
		super.onDestroyView()
	}

	override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) {
		when (key) {
			AppSettings.KEY_TRACKER_NOTIFICATIONS -> updateInfo()
		}
	}

	override fun onPreferenceTreeClick(preference: Preference): Boolean {
		return when (preference.key) {
			AppSettings.KEY_NOTIFICATIONS_SOUND -> {
				ringtonePickContract.launch(settings.notificationSound)
				true
			}

			else -> super.onPreferenceTreeClick(preference)
		}
	}

	private fun updateInfo() {
		findPreference<Preference>(AppSettings.KEY_NOTIFICATIONS_INFO)
			?.isVisible = !settings.isTrackerNotificationsEnabled
	}
}
