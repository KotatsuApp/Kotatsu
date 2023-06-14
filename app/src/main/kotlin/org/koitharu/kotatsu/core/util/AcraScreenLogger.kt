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
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.WeakHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AcraScreenLogger @Inject constructor() : FragmentLifecycleCallbacks(), DefaultActivityLifecycleCallbacks {

	private val timeFormat = SimpleDateFormat.getTimeInstance(DateFormat.DEFAULT, Locale.ROOT)
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
		"${time()}: ${javaClass.simpleName}"
	}

	private fun time() = timeFormat.format(Date())

	@Suppress("DEPRECATION")
	private fun Bundle?.contentToString() = this?.keySet()?.joinToString { k ->
		val v = get(k)
		"$k=$v"
	} ?: toString()
}
