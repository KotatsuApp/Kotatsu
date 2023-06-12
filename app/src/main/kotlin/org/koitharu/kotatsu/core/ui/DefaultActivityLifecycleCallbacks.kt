package org.koitharu.kotatsu.core.ui

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle

interface DefaultActivityLifecycleCallbacks : ActivityLifecycleCallbacks {

	override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

	override fun onActivityStarted(activity: Activity) = Unit

	override fun onActivityResumed(activity: Activity) = Unit

	override fun onActivityPaused(activity: Activity) = Unit

	override fun onActivityStopped(activity: Activity) = Unit

	override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

	override fun onActivityDestroyed(activity: Activity) = Unit
}
