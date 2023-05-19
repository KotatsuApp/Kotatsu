package org.koitharu.kotatsu.settings.utils

import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.StringRes
import org.koitharu.kotatsu.core.util.ext.getParcelableExtraCompat

class RingtonePickContract(@StringRes private val titleResId: Int) : ActivityResultContract<Uri?, Uri?>() {

	override fun createIntent(context: Context, input: Uri?): Intent {
		val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
		intent.putExtra(
			RingtoneManager.EXTRA_RINGTONE_TYPE,
			RingtoneManager.TYPE_NOTIFICATION,
		)
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
		intent.putExtra(
			RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
			Settings.System.DEFAULT_NOTIFICATION_URI,
		)
		if (titleResId != 0) {
			intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, context.getString(titleResId))
		}
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, input)
		return intent
	}

	override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
		return intent?.getParcelableExtraCompat(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
	}
}
