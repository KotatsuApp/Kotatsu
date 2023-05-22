package org.koitharu.kotatsu.main.ui.protect

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import org.acra.dialog.CrashReportDialog
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.DefaultActivityLifecycleCallbacks
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppProtectHelper @Inject constructor(private val settings: AppSettings) :
	DefaultActivityLifecycleCallbacks {

	private var isUnlocked = settings.appPassword.isNullOrEmpty()

	override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
		if (!isUnlocked && activity !is ProtectActivity && activity !is CrashReportDialog) {
			val sourceIntent = Intent(activity, activity.javaClass)
			activity.intent?.let {
				sourceIntent.putExtras(it)
				sourceIntent.action = it.action
				sourceIntent.setDataAndType(it.data, it.type)
			}
			activity.startActivity(ProtectActivity.newIntent(activity, sourceIntent))
			activity.finishAfterTransition()
		}
	}

	override fun onActivityDestroyed(activity: Activity) {
		if (activity !is ProtectActivity && activity.isFinishing && activity.isTaskRoot) {
			restoreLock()
		}
	}

	fun unlock() {
		isUnlocked = true
	}

	private fun restoreLock() {
		isUnlocked = settings.appPassword.isNullOrEmpty()
	}
}
