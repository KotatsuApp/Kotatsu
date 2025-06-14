package org.koitharu.kotatsu.backups.ui

import android.app.Notification
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.PendingIntentCompat
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ErrorReporterReceiver
import org.koitharu.kotatsu.core.nav.AppRouter
import org.koitharu.kotatsu.core.ui.CoroutineIntentService
import org.koitharu.kotatsu.core.util.ext.checkNotificationPermission
import org.koitharu.kotatsu.core.util.ext.getDisplayMessage

abstract class BaseBackupRestoreService : CoroutineIntentService() {

	protected abstract val notificationTag: String

	protected lateinit var notificationManager: NotificationManagerCompat
		private set

	override fun onCreate() {
		super.onCreate()
		notificationManager = NotificationManagerCompat.from(applicationContext)
		createNotificationChannel()
	}

	override fun IntentJobContext.onError(error: Throwable) {
		if (applicationContext.checkNotificationPermission(CHANNEL_ID)) {
			val notification = createErrorNotification(error)
			notificationManager.notify(notificationTag, startId, notification)
		}
	}

	private fun createNotificationChannel() {
		val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_HIGH)
			.setName(getString(R.string.backup_restore))
			.setShowBadge(true)
			.setVibrationEnabled(false)
			.setSound(null, null)
			.setLightsEnabled(false)
			.build()
		notificationManager.createNotificationChannel(channel)
	}

	protected fun createErrorNotification(error: Throwable): Notification {
		val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
			.setPriority(NotificationCompat.PRIORITY_HIGH)
			.setDefaults(0)
			.setSilent(true)
			.setAutoCancel(true)
			.setContentText(error.getDisplayMessage(resources))
			.setSmallIcon(android.R.drawable.stat_notify_error)
		ErrorReporterReceiver.getPendingIntent(applicationContext, error)?.let { reportIntent ->
			notification.addAction(
				R.drawable.ic_alert_outline,
				applicationContext.getString(R.string.report),
				reportIntent,
			)
		}
		notification.setContentIntent(
			PendingIntentCompat.getActivity(
				applicationContext,
				0,
				AppRouter.homeIntent(this),
				0,
				false,
			),
		)
		return notification.build()
	}

	protected companion object {

		const val CHANNEL_ID = "backup_restore"
	}
}
