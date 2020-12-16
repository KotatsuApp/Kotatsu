package org.koitharu.kotatsu.settings

import android.content.Context
import android.media.RingtoneManager
import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import org.koin.android.ext.android.get
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BasePreferenceFragment
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.settings.utils.RingtonePickContract
import org.koitharu.kotatsu.utils.ext.toUriOrNull

class NotificationSettingsLegacyFragment : BasePreferenceFragment(R.string.notifications) {

	private val ringtonePickContract = registerForActivityResult(
		RingtonePickContract(get<Context>().getString(R.string.notification_sound))
	) { uri ->
		settings.notificationSound = uri?.toString().orEmpty()
		findPreference<Preference>(AppSettings.KEY_NOTIFICATIONS_SOUND)?.run {
			summary = RingtoneManager.getRingtone(context, uri).getTitle(context)
		}
	}

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_notifications)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		findPreference<Preference>(AppSettings.KEY_NOTIFICATIONS_SOUND)?.run {
			val uri = settings.notificationSound.toUriOrNull()
			summary = RingtoneManager.getRingtone(context, uri).getTitle(context)
		}
	}

	override fun onPreferenceTreeClick(preference: Preference?): Boolean {
		return when (preference?.key) {
			AppSettings.KEY_NOTIFICATIONS_SOUND -> {
				ringtonePickContract.launch(settings.notificationSound.toUriOrNull())
				true
			}
			else -> super.onPreferenceTreeClick(preference)
		}
	}
}