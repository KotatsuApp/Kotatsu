package org.koitharu.kotatsu.local.ui

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
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
import org.koitharu.kotatsu.details.ui.DetailsActivity
import org.koitharu.kotatsu.local.data.importer.SingleMangaImporter
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import org.koitharu.kotatsu.utils.ext.getDisplayMessage
import org.koitharu.kotatsu.utils.ext.toBitmapOrNull
import org.koitharu.kotatsu.utils.ext.toUriOrNull

@HiltWorker
class ImportWorker @AssistedInject constructor(
	@Assisted appContext: Context,
	@Assisted params: WorkerParameters,
	private val importer: SingleMangaImporter,
	private val coil: ImageLoader
) : CoroutineWorker(appContext, params) {

	private val notificationManager by lazy {
		applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
	}

	override suspend fun doWork(): Result {
		val uri = inputData.getString(DATA_URI)?.toUriOrNull() ?: return Result.failure()
		setForeground(getForegroundInfo())
		val result = runCatchingCancellable {
			importer.import(uri).manga
		}
		val notification = buildNotification(result)
		notificationManager.notify(uri.hashCode(), notification)
		return Result.success()
	}

	override suspend fun getForegroundInfo(): ForegroundInfo {
		val title = applicationContext.getString(R.string.importing_manga)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val channel = NotificationChannel(CHANNEL_ID, title, NotificationManager.IMPORTANCE_LOW)
			channel.setShowBadge(false)
			channel.enableVibration(false)
			channel.setSound(null, null)
			channel.enableLights(false)
			notificationManager.createNotificationChannel(channel)
		}

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

		return ForegroundInfo(FOREGROUND_NOTIFICATION_ID, notification)
	}

	private suspend fun buildNotification(result: kotlin.Result<Manga>): Notification {
		val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
			.setPriority(NotificationCompat.PRIORITY_DEFAULT)
			.setDefaults(0)
			.setSilent(true)
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
			).setAutoCancel(true)
				.setVisibility(
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
