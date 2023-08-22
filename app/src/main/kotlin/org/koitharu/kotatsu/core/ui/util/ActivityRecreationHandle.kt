package org.koitharu.kotatsu.core.ui.util

import android.app.Activity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import org.koitharu.kotatsu.core.ui.DefaultActivityLifecycleCallbacks
import java.util.WeakHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityRecreationHandle @Inject constructor() : DefaultActivityLifecycleCallbacks {

	private val activities = WeakHashMap<Activity, Unit>()

	override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
		activities[activity] = Unit
	}

	override fun onActivityDestroyed(activity: Activity) {
		activities.remove(activity)
	}

	fun recreateAll() {
		val snapshot = activities.keys.toList()
		snapshot.forEach { ActivityCompat.recreate(it) }
	}

	fun recreate(cls: Class<out Activity>) {
		val activity = activities.keys.find { x -> x.javaClass == cls } ?: return
		ActivityCompat.recreate(activity)
	}
}
