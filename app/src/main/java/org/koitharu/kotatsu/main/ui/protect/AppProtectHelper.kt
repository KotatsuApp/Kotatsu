package org.koitharu.kotatsu.main.ui.protect

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import javax.inject.Inject
import javax.inject.Singleton
import org.koitharu.kotatsu.core.prefs.AppSettings

@Singleton
class AppProtectHelper @Inject constructor(private val settings: AppSettings) : Application.ActivityLifecycleCallbacks {

	private var isUnlocked = settings.appPassword.isNullOrEmpty()

	override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
		if (activity !is ProtectActivity && !isUnlocked) {
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

	override fun onActivityStarted(activity: Activity) = Unit

	override fun onActivityResumed(activity: Activity) = Unit

	override fun onActivityPaused(activity: Activity) = Unit

	override fun onActivityStopped(activity: Activity) = Unit

	override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

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
