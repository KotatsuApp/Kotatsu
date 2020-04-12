package org.koitharu.kotatsu.ui.settings

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.preference.Preference
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.ui.common.BasePreferenceFragment
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
				val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
				intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,
					RingtoneManager.TYPE_NOTIFICATION)
				intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
				intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
				intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
					Settings.System.DEFAULT_NOTIFICATION_URI)
				intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, preference.title)
				val existingValue = settings.notificationSound.toUriOrNull()
				intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existingValue)
				startActivityForResult(intent, REQUEST_RINGTONE)
				true
			}
			else -> super.onPreferenceTreeClick(preference)
		}
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		when (requestCode) {
			REQUEST_RINGTONE -> {
				if (resultCode == Activity.RESULT_OK) {
					val uri =
						data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
					settings.notificationSound = uri?.toString().orEmpty()
					findPreference<Preference>(R.string.key_notifications_sound)?.run {
						summary = RingtoneManager.getRingtone(context, uri).getTitle(context)
					}
				}
			}
			else -> {
				super.onActivityResult(requestCode, resultCode, data)
			}
		}
	}

	private companion object {

		const val REQUEST_RINGTONE = 340
	}
}