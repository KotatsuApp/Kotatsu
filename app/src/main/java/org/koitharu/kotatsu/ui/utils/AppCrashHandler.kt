package org.koitharu.kotatsu.ui.utils

import android.content.Context
import android.content.Intent
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

class AppCrashHandler(private val applicationContext: Context) : Thread.UncaughtExceptionHandler {

	override fun uncaughtException(t: Thread, e: Throwable) {
		val crashInfo = buildString {
			val writer = StringWriter()
			e.printStackTrace(PrintWriter(writer))
			append(writer.toString().trimIndent())
		}
		val intent = Intent(applicationContext, CrashActivity::class.java)
		intent.putExtra(Intent.EXTRA_TEXT, crashInfo)
		intent.flags = (Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
		try {
			applicationContext.startActivity(intent)
		} catch (e: Throwable) {
			e.printStackTrace()
		}
		exitProcess(1)
	}
}