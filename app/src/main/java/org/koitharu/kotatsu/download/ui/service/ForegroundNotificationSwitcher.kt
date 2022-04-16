package org.koitharu.kotatsu.download.ui.service

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.SparseArray
import androidx.core.app.ServiceCompat
import androidx.core.util.isEmpty
import androidx.core.util.size

private const val DEFAULT_DELAY = 500L

class ForegroundNotificationSwitcher(
	private val service: Service,
) {

	private val notificationManager = service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
	private val notifications = SparseArray<Notification>()
	private val handler = Handler(Looper.getMainLooper())

	@Synchronized
	fun notify(startId: Int, notification: Notification) {
		if (notifications.isEmpty()) {
			service.startForeground(startId, notification)
		} else {
			notificationManager.notify(startId, notification)
		}
		notifications[startId] = notification
	}

	@Synchronized
	fun detach(startId: Int, notification: Notification?) {
		notifications.remove(startId)
		if (notifications.isEmpty()) {
			ServiceCompat.stopForeground(service, ServiceCompat.STOP_FOREGROUND_DETACH)
		}
		val nextIndex = notifications.size - 1
		if (nextIndex >= 0) {
			val nextStartId = notifications.keyAt(nextIndex)
			val nextNotification = notifications.valueAt(nextIndex)
			service.startForeground(nextStartId, nextNotification)
		}
		handler.postDelayed(NotifyRunnable(startId, notification), DEFAULT_DELAY)
	}

	private inner class NotifyRunnable(
		private val startId: Int,
		private val notification: Notification?,
	) : Runnable {

		override fun run() {
			if (notification != null) {
				notificationManager.notify(startId, notification)
			} else {
				notificationManager.cancel(startId)
			}
		}
	}
}