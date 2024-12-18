package org.koitharu.kotatsu.core

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.BadParcelableException
import androidx.core.app.PendingIntentCompat
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.core.nav.AppRouter
import org.koitharu.kotatsu.core.util.ext.getSerializableExtraCompat
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import org.koitharu.kotatsu.core.util.ext.report

class ErrorReporterReceiver : BroadcastReceiver() {

	override fun onReceive(context: Context?, intent: Intent?) {
		val e = intent?.getSerializableExtraCompat<Throwable>(AppRouter.KEY_ERROR) ?: return
		e.report()
	}

	companion object {

		private const val ACTION_REPORT = "${BuildConfig.APPLICATION_ID}.action.REPORT_ERROR"

		fun getPendingIntent(context: Context, e: Throwable): PendingIntent? = try {
			val intent = Intent(context, ErrorReporterReceiver::class.java)
			intent.setAction(ACTION_REPORT)
			intent.setData(Uri.parse("err://${e.hashCode()}"))
			intent.putExtra(AppRouter.KEY_ERROR, e)
			PendingIntentCompat.getBroadcast(context, 0, intent, 0, false)
		} catch (e: BadParcelableException) {
			e.printStackTraceDebug()
			null
		}
	}
}
