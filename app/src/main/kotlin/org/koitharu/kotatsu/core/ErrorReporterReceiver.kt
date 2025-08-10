package org.koitharu.kotatsu.core

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.net.toUri
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.nav.AppRouter
import org.koitharu.kotatsu.core.util.ext.getSerializableExtraCompat
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import org.koitharu.kotatsu.core.util.ext.report

class ErrorReporterReceiver : BroadcastReceiver() {

	override fun onReceive(context: Context?, intent: Intent?) {
		val e = intent?.getSerializableExtraCompat<Throwable>(AppRouter.KEY_ERROR) ?: return
		val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
		if (notificationId != 0 && context != null) {
			val notificationTag = intent.getStringExtra(EXTRA_NOTIFICATION_TAG)
			NotificationManagerCompat.from(context).cancel(notificationTag, notificationId)
		}
		e.report()
	}

	companion object {

		private const val ACTION_REPORT = "${BuildConfig.APPLICATION_ID}.action.REPORT_ERROR"
		private const val EXTRA_NOTIFICATION_ID = "notify.id"
		private const val EXTRA_NOTIFICATION_TAG = "notify.tag"

		fun getPendingIntent(context: Context, e: Throwable): PendingIntent? = getPendingIntentInternal(
			context = context,
			e = e,
			notificationId = 0,
			notificationTag = null,
		)

		fun getNotificationAction(
			context: Context,
			e: Throwable,
			notificationId: Int,
			notificationTag: String?,
		): NotificationCompat.Action? {
			val intent = getPendingIntentInternal(
				context = context,
				e = e,
				notificationId = notificationId,
				notificationTag = notificationTag,
			) ?: return null
			return NotificationCompat.Action(
				R.drawable.ic_alert_outline,
				context.getString(R.string.report),
				intent,
			)
		}

		private fun getPendingIntentInternal(
			context: Context,
			e: Throwable,
			notificationId: Int,
			notificationTag: String?,
		): PendingIntent? = runCatching {
			val intent = Intent(context, ErrorReporterReceiver::class.java)
			intent.setAction(ACTION_REPORT)
			intent.setData("err://${e.hashCode()}".toUri())
			intent.putExtra(AppRouter.KEY_ERROR, e)
			intent.putExtra(EXTRA_NOTIFICATION_ID, notificationId)
			intent.putExtra(EXTRA_NOTIFICATION_TAG, notificationTag)
			PendingIntentCompat.getBroadcast(context, 0, intent, 0, false)
		}.onFailure { e ->
			// probably cannot write exception as serializable
			e.printStackTraceDebug()
		}.getOrNull()
	}
}
