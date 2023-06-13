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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AcraScreenLogger @Inject constructor() : FragmentLifecycleCallbacks(), DefaultActivityLifecycleCallbacks {

	private val timeFormat = SimpleDateFormat.getTimeInstance(DateFormat.DEFAULT, Locale.ROOT)

	override fun onFragmentAttached(fm: FragmentManager, f: Fragment, context: Context) {
		super.onFragmentAttached(fm, f, context)
		ACRA.errorReporter.putCustomData(f.key(), "${time()}: ${f.arguments}")
	}

	override fun onFragmentDetached(fm: FragmentManager, f: Fragment) {
		super.onFragmentDetached(fm, f)
		ACRA.errorReporter.removeCustomData(f.key())
	}

	override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
		super.onActivityCreated(activity, savedInstanceState)
		ACRA.errorReporter.putCustomData(activity.key(), "${time()}: ${activity.intent}")
		(activity as? FragmentActivity)?.supportFragmentManager?.registerFragmentLifecycleCallbacks(this, true)
	}

	override fun onActivityDestroyed(activity: Activity) {
		super.onActivityDestroyed(activity)
		ACRA.errorReporter.removeCustomData(activity.key())
	}

	private fun Activity.key() = "Activity[${javaClass.simpleName}]"

	private fun Fragment.key() = "Fragment[${javaClass.simpleName}]"

	private fun time() = timeFormat.format(Date())
}
