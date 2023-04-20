package org.koitharu.kotatsu.download.ui.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.text.format.DateUtils
import android.util.SparseArray
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.text.HtmlCompat
import androidx.core.text.htmlEncode
import androidx.core.text.parseAsHtml
import androidx.core.util.forEach
import androidx.core.util.isNotEmpty
import androidx.core.util.size
import com.google.android.material.R as materialR
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.details.ui.DetailsActivity
import org.koitharu.kotatsu.download.domain.DownloadState
import org.koitharu.kotatsu.download.ui.DownloadsActivity
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.util.ellipsize
import org.koitharu.kotatsu.parsers.util.format
import org.koitharu.kotatsu.search.ui.MangaListActivity
import org.koitharu.kotatsu.utils.ext.getDisplayMessage

class DownloadNotification(private val context: Context) {

	private val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
	private val states = SparseArray<DownloadState>()
	private val groupBuilder = NotificationCompat.Builder(context, CHANNEL_ID)

	private val queueIntent = PendingIntentCompat.getActivity(
		context,
		REQUEST_QUEUE,
		DownloadsActivity.newIntent(context),
		0,
		false,
	)

	private val localListIntent = PendingIntentCompat.getActivity(
		context,
		REQUEST_LIST_LOCAL,
		MangaListActivity.newIntent(context, MangaSource.LOCAL),
		0,
		false,
	)

	init {
		groupBuilder.setOnlyAlertOnce(true)
		groupBuilder.setDefaults(0)
		groupBuilder.color = ContextCompat.getColor(context, R.color.blue_primary)
		groupBuilder.foregroundServiceBehavior = NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
		groupBuilder.setSilent(true)
		groupBuilder.setGroup(GROUP_ID)
		groupBuilder.setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
		groupBuilder.setGroupSummary(true)
		groupBuilder.setContentTitle(context.getString(R.string.downloading_manga))
	}

	fun buildGroupNotification(): Notification {
		val style = NotificationCompat.InboxStyle(groupBuilder)
		var progress = 0f
		var isAllDone = true
		var isInProgress = false
		groupBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
		states.forEach { _, state ->
			if (state.manga.isNsfw) {
				groupBuilder.setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
			}
			val summary = when (state) {
				is DownloadState.Cancelled -> {
					progress++
					context.getString(R.string.cancelling_)
				}

				is DownloadState.Done -> {
					progress++
					context.getString(R.string.download_complete)
				}

				is DownloadState.Error -> {
					isAllDone = false
					context.getString(R.string.error)
				}

				is DownloadState.PostProcessing -> {
					progress++
					isInProgress = true
					isAllDone = false
					context.getString(R.string.processing_)
				}

				is DownloadState.Preparing -> {
					isAllDone = false
					isInProgress = true
					context.getString(R.string.preparing_)
				}

				is DownloadState.Progress -> {
					isAllDone = false
					isInProgress = true
					progress += state.percent
					context.getString(R.string.percent_string_pattern, (state.percent * 100).format())
				}

				is DownloadState.Queued -> {
					isAllDone = false
					isInProgress = true
					context.getString(R.string.queued)
				}
			}
			style.addLine(
				context.getString(
					R.string.download_summary_pattern,
					state.manga.title.ellipsize(16).htmlEncode(),
					summary.htmlEncode(),
				).parseAsHtml(HtmlCompat.FROM_HTML_MODE_LEGACY),
			)
		}
		progress = if (isInProgress) {
			progress / states.size.toFloat()
		} else {
			1f
		}
		style.setBigContentTitle(
			context.getString(if (isAllDone) R.string.download_complete else R.string.downloading_manga),
		)
		groupBuilder.setContentText(context.resources.getQuantityString(R.plurals.items, states.size, states.size()))
		groupBuilder.setNumber(states.size)
		groupBuilder.setSmallIcon(
			if (isInProgress) android.R.drawable.stat_sys_download else android.R.drawable.stat_sys_download_done,
		)
		groupBuilder.setContentIntent(if (isAllDone) localListIntent else queueIntent)
		groupBuilder.setAutoCancel(isAllDone)
		when (progress) {
			1f -> groupBuilder.setProgress(0, 0, false)
			0f -> groupBuilder.setProgress(1, 0, true)
			else -> groupBuilder.setProgress(100, (progress * 100f).toInt(), false)
		}
		return groupBuilder.build()
	}

	fun detach() {
		if (states.isNotEmpty()) {
			val notification = buildGroupNotification()
			manager.notify(ID_GROUP_DETACHED, notification)
		}
		manager.cancel(ID_GROUP)
	}

	fun newItem(startId: Int) = Item(startId)

	inner class Item(
		private val startId: Int,
	) {

		private val builder = NotificationCompat.Builder(context, CHANNEL_ID)
		private val cancelAction = NotificationCompat.Action(
			materialR.drawable.material_ic_clear_black_24dp,
			context.getString(android.R.string.cancel),
			PendingIntentCompat.getBroadcast(
				context,
				startId * 2,
				DownloadService.getCancelIntent(startId),
				PendingIntent.FLAG_CANCEL_CURRENT,
				false,
			),
		)
		private val retryAction = NotificationCompat.Action(
			R.drawable.ic_restart_black,
			context.getString(R.string.try_again),
			PendingIntentCompat.getBroadcast(
				context,
				startId * 2 + 1,
				DownloadService.getResumeIntent(startId),
				PendingIntent.FLAG_CANCEL_CURRENT,
				false,
			),
		)

		init {
			builder.setOnlyAlertOnce(true)
			builder.setDefaults(0)
			builder.color = ContextCompat.getColor(context, R.color.blue_primary)
			builder.foregroundServiceBehavior = NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
			builder.setSilent(true)
			builder.setGroup(GROUP_ID)
			builder.setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
		}

		fun notify(state: DownloadState, timeLeft: Long) {
			builder.setContentTitle(state.manga.title)
			builder.setContentText(context.getString(R.string.manga_downloading_))
			builder.setProgress(1, 0, true)
			builder.setSmallIcon(android.R.drawable.stat_sys_download)
			builder.setContentIntent(queueIntent)
			builder.setStyle(null)
			builder.setLargeIcon(state.cover?.toBitmap())
			builder.clearActions()
			builder.setSubText(null)
			builder.setShowWhen(false)
			builder.setVisibility(
				if (state.manga.isNsfw) {
					NotificationCompat.VISIBILITY_PRIVATE
				} else {
					NotificationCompat.VISIBILITY_PUBLIC
				},
			)
			when (state) {
				is DownloadState.Cancelled -> {
					builder.setProgress(1, 0, true)
					builder.setContentText(context.getString(R.string.cancelling_))
					builder.setContentIntent(null)
					builder.setStyle(null)
					builder.setOngoing(true)
					builder.priority = NotificationCompat.PRIORITY_DEFAULT
				}

				is DownloadState.Done -> {
					builder.setProgress(0, 0, false)
					builder.setContentText(context.getString(R.string.download_complete))
					builder.setContentIntent(createMangaIntent(context, state.localManga))
					builder.setAutoCancel(true)
					builder.setSmallIcon(android.R.drawable.stat_sys_download_done)
					builder.setCategory(null)
					builder.setStyle(null)
					builder.setOngoing(false)
					builder.setShowWhen(true)
					builder.setWhen(System.currentTimeMillis())
					builder.priority = NotificationCompat.PRIORITY_DEFAULT
				}

				is DownloadState.Error -> {
					val message = state.error.getDisplayMessage(context.resources)
					builder.setProgress(0, 0, false)
					builder.setSmallIcon(android.R.drawable.stat_notify_error)
					builder.setSubText(context.getString(R.string.error))
					builder.setContentText(message)
					builder.setAutoCancel(!state.canRetry)
					builder.setOngoing(state.canRetry)
					builder.setCategory(NotificationCompat.CATEGORY_ERROR)
					builder.setShowWhen(true)
					builder.setWhen(System.currentTimeMillis())
					builder.setStyle(NotificationCompat.BigTextStyle().bigText(message))
					if (state.canRetry) {
						builder.addAction(cancelAction)
						builder.addAction(retryAction)
					}
					builder.priority = NotificationCompat.PRIORITY_DEFAULT
				}

				is DownloadState.PostProcessing -> {
					builder.setProgress(1, 0, true)
					builder.setContentText(context.getString(R.string.processing_))
					builder.setStyle(null)
					builder.setOngoing(true)
					builder.priority = NotificationCompat.PRIORITY_DEFAULT
				}

				is DownloadState.Queued -> {
					builder.setProgress(0, 0, false)
					builder.setContentText(context.getString(R.string.queued))
					builder.setStyle(null)
					builder.setOngoing(true)
					builder.addAction(cancelAction)
					builder.priority = NotificationCompat.PRIORITY_LOW
				}

				is DownloadState.Preparing -> {
					builder.setProgress(1, 0, true)
					builder.setContentText(context.getString(R.string.preparing_))
					builder.setStyle(null)
					builder.setOngoing(true)
					builder.addAction(cancelAction)
					builder.priority = NotificationCompat.PRIORITY_DEFAULT
				}

				is DownloadState.Progress -> {
					builder.setProgress(state.max, state.progress, false)
					val percent = context.getString(R.string.percent_string_pattern, (state.percent * 100).format())
					if (timeLeft > 0L) {
						val eta = DateUtils.getRelativeTimeSpanString(timeLeft, 0L, DateUtils.SECOND_IN_MILLIS)
						builder.setContentText(eta)
						builder.setSubText(percent)
					} else {
						builder.setContentText(percent)
					}
					builder.setCategory(NotificationCompat.CATEGORY_PROGRESS)
					builder.setStyle(null)
					builder.setOngoing(true)
					builder.addAction(cancelAction)
					builder.priority = NotificationCompat.PRIORITY_DEFAULT
				}
			}
			val notification = builder.build()
			states.append(startId, state)
			updateGroupNotification()
			manager.notify(TAG, startId, notification)
		}

		fun dismiss() {
			manager.cancel(TAG, startId)
			states.remove(startId)
			updateGroupNotification()
		}
	}

	private fun updateGroupNotification() {
		val notification = buildGroupNotification()
		manager.notify(ID_GROUP, notification)
	}

	private fun createMangaIntent(context: Context, manga: Manga) = PendingIntentCompat.getActivity(
		context,
		manga.hashCode(),
		DetailsActivity.newIntent(context, manga),
		PendingIntent.FLAG_CANCEL_CURRENT,
		false,
	)

	companion object {

		private const val TAG = "download"
		private const val CHANNEL_ID = "download"
		private const val GROUP_ID = "downloads"
		private const val REQUEST_QUEUE = 6
		private const val REQUEST_LIST_LOCAL = 7
		const val ID_GROUP = 9999
		private const val ID_GROUP_DETACHED = 9998

		fun createChannel(context: Context) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				val manager = NotificationManagerCompat.from(context)
				if (manager.getNotificationChannel(CHANNEL_ID) == null) {
					val channel = NotificationChannel(
						CHANNEL_ID,
						context.getString(R.string.downloads),
						NotificationManager.IMPORTANCE_LOW,
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
