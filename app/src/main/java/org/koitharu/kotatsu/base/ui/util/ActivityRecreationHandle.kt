package org.koitharu.kotatsu.base.ui.util

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import java.util.*

class ActivityRecreationHandle : ActivityLifecycleCallbacks {

	private val activities = WeakHashMap<Activity, Unit>()

	override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
		activities[activity] = Unit
	}

	override fun onActivityStarted(activity: Activity) = Unit

	override fun onActivityResumed(activity: Activity) = Unit

	override fun onActivityPaused(activity: Activity) = Unit

	override fun onActivityStopped(activity: Activity) = Unit

	override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

	override fun onActivityDestroyed(activity: Activity) {
		activities.remove(activity)
	}

	fun recreateAll() {
		val snapshot = activities.keys.toList()
		snapshot.forEach { it.recreate() }
	}
}