package org.koitharu.kotatsu.main.ui.protect

import android.app.Activity
import android.content.Intent
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.main.ui.MainActivity

class AppProtectHelper(private val settings: AppSettings) {

	private var isUnlocked = settings.appPassword.isNullOrEmpty()

	fun unlock(activity: Activity) {
		isUnlocked = true
		with(activity) {
			startActivity(Intent(this, MainActivity::class.java)
				.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
		}
	}

	fun lock() {
		isUnlocked = settings.appPassword.isNullOrEmpty()
	}

	fun check(activity: Activity): Boolean {
		return if (!isUnlocked) {
			activity.startActivity(ProtectActivity.newIntent(activity)
				.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
			true
		} else {
			false
		}
	}
}