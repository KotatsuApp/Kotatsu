package org.koitharu.kotatsu.download.ui.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.details.ui.DetailsActivity
import org.koitharu.kotatsu.download.domain.DownloadManager
import org.koitharu.kotatsu.download.ui.DownloadsActivity
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.utils.PendingIntentCompat
import org.koitharu.kotatsu.utils.ext.format
import org.koitharu.kotatsu.utils.ext.getDisplayMessage
import com.google.android.material.R as materialR

class DownloadNotification(
	private val context: Context,
	startId: Int,
) {

	private val builder = NotificationCompat.Builder(context, CHANNEL_ID)
	private val cancelAction = NotificationCompat.Action(
		materialR.drawable.material_ic_clear_black_24dp,
		context.getString(android.R.string.cancel),
		PendingIntent.getBroadcast(
			context,
			startId,
			DownloadService.getCancelIntent(startId),
			PendingIntent.FLAG_CANCEL_CURRENT or PendingIntentCompat.FLAG_IMMUTABLE
		)
	)
	private val listIntent = PendingIntent.getActivity(
		context,
		REQUEST_LIST,
		DownloadsActivity.newIntent(context),
		PendingIntentCompat.FLAG_IMMUTABLE,
	)

	init {
		builder.setOnlyAlertOnce(true)
		builder.setDefaults(0)
		builder.color = ContextCompat.getColor(context, R.color.blue_primary)
	}

	fun create(state: DownloadManager.State): Notification {
		builder.setContentTitle(state.manga.title)
		builder.setContentText(context.getString(R.string.manga_downloading_))
		builder.setProgress(1, 0, true)
		builder.setSmallIcon(android.R.drawable.stat_sys_download)
		builder.setContentIntent(listIntent)
		builder.setStyle(null)
		builder.setLargeIcon(state.cover?.toBitmap())
		builder.clearActions()
		when (state) {
			is DownloadManager.State.Cancelling -> {
				builder.setProgress(1, 0, true)
				builder.setContentText(context.getString(R.string.cancelling_))
				builder.setContentIntent(null)
				builder.setStyle(null)
			}
			is DownloadManager.State.Done -> {
				builder.setProgress(0, 0, false)
				builder.setContentText(context.getString(R.string.download_complete))
				builder.setContentIntent(createMangaIntent(context, state.localManga))
				builder.setAutoCancel(true)
				builder.setSmallIcon(android.R.drawable.stat_sys_download_done)
				builder.setCategory(null)
				builder.setStyle(null)
			}
			is DownloadManager.State.Error -> {
				val message = state.error.getDisplayMessage(context.resources)
				builder.setProgress(0, 0, false)
				builder.setSmallIcon(android.R.drawable.stat_notify_error)
				builder.setSubText(context.getString(R.string.error))
				builder.setContentText(message)
				builder.setAutoCancel(true)
				builder.setCategory(NotificationCompat.CATEGORY_ERROR)
				builder.setStyle(NotificationCompat.BigTextStyle().bigText(message))
			}
			is DownloadManager.State.PostProcessing -> {
				builder.setProgress(1, 0, true)
				builder.setContentText(context.getString(R.string.processing_))
				builder.setStyle(null)
			}
			is DownloadManager.State.Queued,
			is DownloadManager.State.Preparing -> {
				builder.setProgress(1, 0, true)
				builder.setContentText(context.getString(R.string.preparing_))
				builder.setStyle(null)
				builder.addAction(cancelAction)
			}
			is DownloadManager.State.Progress -> {
				builder.setProgress(state.max, state.progress, false)
				builder.setContentText((state.percent * 100).format() + "%")
				builder.setCategory(NotificationCompat.CATEGORY_PROGRESS)
				builder.setStyle(null)
				builder.addAction(cancelAction)
			}
			is DownloadManager.State.WaitingForNetwork -> {
				builder.setProgress(0, 0, false)
				builder.setContentText(context.getString(R.string.waiting_for_network))
				builder.setStyle(null)
				builder.addAction(cancelAction)
			}
		}
		return builder.build()
	}

	private fun createMangaIntent(context: Context, manga: Manga) = PendingIntent.getActivity(
		context,
		manga.hashCode(),
		DetailsActivity.newIntent(context, manga),
		PendingIntent.FLAG_CANCEL_CURRENT or PendingIntentCompat.FLAG_IMMUTABLE
	)

	companion object {

		private const val CHANNEL_ID = "download"
		private const val REQUEST_LIST = 6

		fun createChannel(context: Context) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				val manager = NotificationManagerCompat.from(context)
				if (manager.getNotificationChannel(CHANNEL_ID) == null) {
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
			}
		}
	}
}