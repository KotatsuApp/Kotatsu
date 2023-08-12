package org.koitharu.kotatsu.core.util

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks
import org.acra.ACRA
import org.koitharu.kotatsu.core.ui.DefaultActivityLifecycleCallbacks
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.WeakHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AcraScreenLogger @Inject constructor() : FragmentLifecycleCallbacks(), DefaultActivityLifecycleCallbacks {

	private val keys = WeakHashMap<Any, String>()

	override fun onFragmentAttached(fm: FragmentManager, f: Fragment, context: Context) {
		super.onFragmentAttached(fm, f, context)
		ACRA.errorReporter.putCustomData(f.key(), f.arguments.contentToString())
	}

	override fun onFragmentDetached(fm: FragmentManager, f: Fragment) {
		super.onFragmentDetached(fm, f)
		ACRA.errorReporter.removeCustomData(f.key())
		keys.remove(f)
	}

	override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
		super.onActivityCreated(activity, savedInstanceState)
		ACRA.errorReporter.putCustomData(activity.key(), activity.intent.extras.contentToString())
		(activity as? FragmentActivity)?.supportFragmentManager?.registerFragmentLifecycleCallbacks(this, true)
	}

	override fun onActivityDestroyed(activity: Activity) {
		super.onActivityDestroyed(activity)
		ACRA.errorReporter.removeCustomData(activity.key())
		keys.remove(activity)
	}

	private fun Any.key() = keys.getOrPut(this) {
		val time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS)
		"$time: ${javaClass.simpleName}"
	}

	@Suppress("DEPRECATION")
	private fun Bundle?.contentToString() = this?.keySet()?.joinToString { k ->
		val v = get(k)
		"$k=$v"
	} ?: toString()
}
