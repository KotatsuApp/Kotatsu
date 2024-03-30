package org.koitharu.kotatsu.local.ui

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.PendingIntentCompat
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import coil.ImageLoader
import coil.request.ImageRequest
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ErrorReporterReceiver
import org.koitharu.kotatsu.core.util.ext.checkNotificationPermission
import org.koitharu.kotatsu.core.util.ext.getDisplayMessage
import org.koitharu.kotatsu.core.util.ext.toBitmapOrNull
import org.koitharu.kotatsu.core.util.ext.toUriOrNull
import org.koitharu.kotatsu.details.ui.DetailsActivity
import org.koitharu.kotatsu.local.data.importer.SingleMangaImporter
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable

@HiltWorker
class ImportWorker @AssistedInject constructor(
	@Assisted appContext: Context,
	@Assisted params: WorkerParameters,
	private val importer: SingleMangaImporter,
	private val coil: ImageLoader
) : CoroutineWorker(appContext, params) {

	private val notificationManager by lazy { NotificationManagerCompat.from(appContext) }

	override suspend fun doWork(): Result {
		val uri = inputData.getString(DATA_URI)?.toUriOrNull() ?: return Result.failure()
		setForeground(getForegroundInfo())
		val result = runCatchingCancellable {
			importer.import(uri).manga
		}
		if (applicationContext.checkNotificationPermission(CHANNEL_ID)) {
			val notification = buildNotification(result)
			notificationManager.notify(uri.hashCode(), notification)
		}
		return Result.success()
	}

	override suspend fun getForegroundInfo(): ForegroundInfo {
		val title = applicationContext.getString(R.string.importing_manga)
		val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
			.setName(title)
			.setShowBadge(false)
			.setVibrationEnabled(false)
			.setSound(null, null)
			.setLightsEnabled(false)
			.build()
		notificationManager.createNotificationChannel(channel)

		val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
			.setContentTitle(title)
			.setPriority(NotificationCompat.PRIORITY_MIN)
			.setDefaults(0)
			.setSilent(true)
			.setOngoing(true)
			.setProgress(0, 0, true)
			.setSmallIcon(android.R.drawable.stat_sys_download)
			.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
			.setCategory(NotificationCompat.CATEGORY_PROGRESS)
			.build()

		return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			ForegroundInfo(FOREGROUND_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
		} else {
			ForegroundInfo(FOREGROUND_NOTIFICATION_ID, notification)
		}
	}

	private suspend fun buildNotification(result: kotlin.Result<Manga>): Notification {
		val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
			.setPriority(NotificationCompat.PRIORITY_DEFAULT)
			.setDefaults(0)
			.setSilent(true)
			.setAutoCancel(true)
		result.onSuccess { manga ->
			notification.setLargeIcon(
				coil.execute(
					ImageRequest.Builder(applicationContext)
						.data(manga.coverUrl)
						.tag(manga.source)
						.build(),
				).toBitmapOrNull(),
			)
			notification.setSubText(manga.title)
			val intent = DetailsActivity.newIntent(applicationContext, manga)
			notification.setContentIntent(
				PendingIntentCompat.getActivity(
					applicationContext,
					manga.id.toInt(),
					intent,
					PendingIntent.FLAG_UPDATE_CURRENT,
					false,
				),
			).setVisibility(
				if (manga.isNsfw) NotificationCompat.VISIBILITY_SECRET else NotificationCompat.VISIBILITY_PUBLIC,
			)
			notification.setContentTitle(applicationContext.getString(R.string.import_completed))
				.setContentText(applicationContext.getString(R.string.import_completed_hint))
				.setSmallIcon(R.drawable.ic_stat_done)
			NotificationCompat.BigTextStyle(notification)
				.bigText(applicationContext.getString(R.string.import_completed_hint))
		}.onFailure { error ->
			notification.setContentTitle(applicationContext.getString(R.string.error_occurred))
				.setContentText(error.getDisplayMessage(applicationContext.resources))
				.setSmallIcon(android.R.drawable.stat_notify_error)
				.addAction(
					R.drawable.ic_alert_outline,
					applicationContext.getString(R.string.report),
					ErrorReporterReceiver.getPendingIntent(applicationContext, error),
				)
		}
		return notification.build()
	}

	companion object {

		const val DATA_URI = "uri"

		private const val TAG = "import"
		private const val CHANNEL_ID = "importing"
		private const val FOREGROUND_NOTIFICATION_ID = 37

		fun start(context: Context, uris: Iterable<Uri>) {
			val constraints = Constraints.Builder()
				.setRequiresStorageNotLow(true)
				.build()
			val requests = uris.map { uri ->
				OneTimeWorkRequestBuilder<ImportWorker>()
					.setConstraints(constraints)
					.addTag(TAG)
					.setInputData(Data.Builder().putString(DATA_URI, uri.toString()).build())
					.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
					.build()
			}
			WorkManager.getInstance(context)
				.enqueue(requests)
		}
	}
}
