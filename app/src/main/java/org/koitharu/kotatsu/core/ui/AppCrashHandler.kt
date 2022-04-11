package org.koitharu.kotatsu.core.ui

import android.content.Context
import android.content.Intent
import android.util.Log
import kotlin.system.exitProcess

class AppCrashHandler(private val applicationContext: Context) : Thread.UncaughtExceptionHandler {

	override fun uncaughtException(t: Thread, e: Throwable) {
		val intent = CrashActivity.newIntent(applicationContext, e)
		intent.flags = (Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
		try {
			applicationContext.startActivity(intent)
		} catch (t: Throwable) {
			t.printStackTrace()
		}
		Log.e("CRASH", e.message, e)
		exitProcess(1)
	}
}