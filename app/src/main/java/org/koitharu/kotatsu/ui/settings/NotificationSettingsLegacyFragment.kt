package org.koitharu.kotatsu.ui.settings

import android.media.RingtoneManager
import android.os.Bundle
import androidx.preference.Preference
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.ui.common.BasePreferenceFragment
import org.koitharu.kotatsu.ui.settings.utils.RingtonePickContract
import org.koitharu.kotatsu.utils.ext.toUriOrNull

class NotificationSettingsLegacyFragment : BasePreferenceFragment(R.string.notifications) {

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_notifications)
		findPreference<Preference>(R.string.key_notifications_sound)?.run {
			val uri = settings.notificationSound.toUriOrNull()
			summary = RingtoneManager.getRingtone(context, uri).getTitle(context)
		}
	}

	override fun onPreferenceTreeClick(preference: Preference?): Boolean {
		return when (preference?.key) {
			getString(R.string.key_notifications_sound) -> {
				registerForActivityResult(RingtonePickContract(preference.title.toString())) { uri ->
					settings.notificationSound = uri?.toString().orEmpty()
					findPreference<Preference>(R.string.key_notifications_sound)?.run {
						summary = RingtoneManager.getRingtone(context, uri).getTitle(context)
					}
				}.launch(settings.notificationSound.toUriOrNull())
				true
			}
			else -> super.onPreferenceTreeClick(preference)
		}
	}
}