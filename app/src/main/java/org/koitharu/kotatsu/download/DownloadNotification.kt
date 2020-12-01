package org.koitharu.kotatsu.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.details.ui.DetailsActivity
import org.koitharu.kotatsu.utils.ext.getDisplayMessage
import kotlin.math.roundToInt

class DownloadNotification(private val context: Context) {

	private val builder = NotificationCompat.Builder(context, CHANNEL_ID)
	private val manager =
		context.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

	init {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
			&& manager.getNotificationChannel(CHANNEL_ID) == null
		) {
			val channel = NotificationChannel(
				CHANNEL_ID,
				context.getString(R.string.downloads),
				NotificationManager.IMPORTANCE_LOW
			)
			channel.enableVibration(false)
			channel.enableLights(false)
			channel.setSound(null, null)
			manager.createNotificationChannel(channel)
		}
		builder.setOnlyAlertOnce(true)
		builder.setDefaults(0)
		builder.color = ContextCompat.getColor(context, R.color.blue_primary)
	}

	fun fillFrom(manga: Manga) {
		builder.setContentTitle(manga.title)
		builder.setContentText(context.getString(R.string.manga_downloading_))
		builder.setProgress(1, 0, true)
		builder.setSmallIcon(android.R.drawable.stat_sys_download)
		builder.setLargeIcon(null)
		builder.setContentIntent(null)
		builder.setStyle(null)
	}

	fun setCancelId(startId: Int) {
		if (startId == 0) {
			builder.clearActions()
		} else {
			val intent = DownloadService.getCancelIntent(context, startId)
			builder.addAction(
				R.drawable.ic_cross,
				context.getString(android.R.string.cancel),
				PendingIntent.getService(
					context,
					startId,
					intent,
					PendingIntent.FLAG_CANCEL_CURRENT
				)
			)
		}
	}

	fun setError(e: Throwable) {
		val message = e.getDisplayMessage(context.resources)
		builder.setProgress(0, 0, false)
		builder.setSmallIcon(android.R.drawable.stat_notify_error)
		builder.setSubText(context.getString(R.string.error))
		builder.setContentText(message)
		builder.setAutoCancel(true)
		builder.setContentIntent(null)
		builder.setCategory(NotificationCompat.CATEGORY_ERROR)
		builder.setStyle(NotificationCompat.BigTextStyle().bigText(message))
	}

	fun setLargeIcon(icon: Drawable?) {
		builder.setLargeIcon(icon?.toBitmap())
	}

	fun setProgress(chaptersTotal: Int, pagesTotal: Int, chapter: Int, page: Int) {
		val max = chaptersTotal * PROGRESS_STEP
		val progress =
			chapter * PROGRESS_STEP + (page / pagesTotal.toFloat() * PROGRESS_STEP).roundToInt()
		val percent = (progress / max.toFloat() * 100).roundToInt()
		builder.setProgress(max, progress, false)
		builder.setContentText("%d%%".format(percent))
		builder.setCategory(NotificationCompat.CATEGORY_PROGRESS)
		builder.setStyle(null)
	}

	fun setWaitingForNetwork() {
		builder.setProgress(0, 0, false)
		builder.setContentText(context.getString(R.string.waiting_for_network))
		builder.setStyle(null)
	}

	fun setPostProcessing() {
		builder.setProgress(1, 0, true)
		builder.setContentText(context.getString(R.string.processing_))
		builder.setStyle(null)
	}

	fun setDone(manga: Manga) {
		builder.setProgress(0, 0, false)
		builder.setContentText(context.getString(R.string.download_complete))
		builder.setContentIntent(createIntent(context, manga))
		builder.setAutoCancel(true)
		builder.setSmallIcon(android.R.drawable.stat_sys_download_done)
		builder.setCategory(null)
		builder.setStyle(null)
	}

	fun setCancelling() {
		builder.setProgress(1, 0, true)
		builder.setContentText(context.getString(R.string.cancelling_))
		builder.setContentIntent(null)
		builder.setStyle(null)
	}

	fun update(id: Int = NOTIFICATION_ID) {
		manager.notify(id, builder.build())
	}

	fun dismiss(id: Int = NOTIFICATION_ID) {
		manager.cancel(id)
	}

	operator fun invoke(): Notification = builder.build()

	companion object {

		const val NOTIFICATION_ID = 201
		const val CHANNEL_ID = "download"

		private const val PROGRESS_STEP = 20

		private fun createIntent(context: Context, manga: Manga) = PendingIntent.getActivity(
			context,
			manga.hashCode(),
			DetailsActivity.newIntent(context, manga),
			PendingIntent.FLAG_CANCEL_CURRENT
		)
	}
}