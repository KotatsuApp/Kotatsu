package org.koitharu.kotatsu.main.ui.protect

import android.app.Activity
import android.content.Intent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.main.ui.MainActivity

@Deprecated("TODO not object")
object AppProtectHelper : KoinComponent {

	val settings by inject<AppSettings>()
	private var isUnlocked = settings.appPassword.isNullOrEmpty()

	fun unlock(activity: Activity) {
		isUnlocked = true
		with(activity) {
			startActivity(Intent(this, MainActivity::class.java))
			finishAfterTransition()
		}
	}

	fun lock() {
		isUnlocked = settings.appPassword.isNullOrEmpty()
	}

	fun check(activity: Activity): Boolean {
		return if (!isUnlocked) {
			with(activity) {
				startActivity(ProtectActivity.newIntent(this))
				finishAfterTransition()
			}
			true
		} else {
			false
		}
	}
}