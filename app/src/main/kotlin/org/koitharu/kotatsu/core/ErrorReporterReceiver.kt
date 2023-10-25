package org.koitharu.kotatsu.core

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.PendingIntentCompat
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.core.util.ext.getSerializableExtraCompat
import org.koitharu.kotatsu.core.util.ext.report

class ErrorReporterReceiver : BroadcastReceiver() {

	override fun onReceive(context: Context?, intent: Intent?) {
		val e = intent?.getSerializableExtraCompat<Throwable>(EXTRA_ERROR) ?: return
		e.report()
	}

	companion object {

		private const val EXTRA_ERROR = "err"
		private const val ACTION_REPORT = "${BuildConfig.APPLICATION_ID}.action.REPORT_ERROR"

		fun getPendingIntent(context: Context, e: Throwable): PendingIntent {
			val intent = Intent(context, ErrorReporterReceiver::class.java)
			intent.setAction(ACTION_REPORT)
			intent.setData(Uri.parse("err://${e.hashCode()}"))
			intent.putExtra(EXTRA_ERROR, e)
			return checkNotNull(PendingIntentCompat.getBroadcast(context, 0, intent, 0, false))
		}
	}
}
