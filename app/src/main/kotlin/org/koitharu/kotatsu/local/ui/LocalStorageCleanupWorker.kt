package org.koitharu.kotatsu.local.ui

import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.PendingIntentCompat
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.await
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.nav.AppRouter
import org.koitharu.kotatsu.core.parser.MangaDataRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.local.data.LocalMangaRepository
import org.koitharu.kotatsu.local.domain.DeleteReadChaptersUseCase
import java.util.concurrent.TimeUnit

@HiltWorker
class LocalStorageCleanupWorker @AssistedInject constructor(
	@Assisted appContext: Context,
	@Assisted params: WorkerParameters,
	private val settings: AppSettings,
	private val localMangaRepository: LocalMangaRepository,
	private val dataRepository: MangaDataRepository,
	private val deleteReadChaptersUseCase: DeleteReadChaptersUseCase,
) : CoroutineWorker(appContext, params) {

	override suspend fun doWork(): Result {
		if (settings.isAutoLocalChaptersCleanupEnabled) {
			deleteReadChaptersUseCase.invoke()
		}
		return if (localMangaRepository.cleanup()) {
			dataRepository.cleanupLocalManga()
			Result.success()
		} else {
			Result.retry()
		}
	}

	override suspend fun getForegroundInfo(): ForegroundInfo {
		val title = applicationContext.getString(R.string.local_storage_cleanup)
		val channel = NotificationChannelCompat.Builder(WORKER_CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
			.setName(title)
			.setShowBadge(true)
			.setVibrationEnabled(false)
			.setSound(null, null)
			.setLightsEnabled(true)
			.build()
		NotificationManagerCompat.from(applicationContext).createNotificationChannel(channel)

		val notification = NotificationCompat.Builder(applicationContext, WORKER_CHANNEL_ID)
			.setContentTitle(title)
			.setContentIntent(
				PendingIntentCompat.getActivity(
					applicationContext,
					0,
					AppRouter.suggestionsSettingsIntent(applicationContext),
					0,
					false,
				),
			)
			.setPriority(NotificationCompat.PRIORITY_MIN)
			.setCategory(NotificationCompat.CATEGORY_SERVICE)
			.setDefaults(0)
			.setOngoing(false)
			.setSilent(true)
			.setProgress(0, 0, true)
			.setSmallIcon(android.R.drawable.stat_notify_sync)
			.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_DEFERRED)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val actionIntent = PendingIntentCompat.getActivity(
				applicationContext, SETTINGS_ACTION_CODE,
				Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
					.putExtra(Settings.EXTRA_APP_PACKAGE, applicationContext.packageName)
					.putExtra(Settings.EXTRA_CHANNEL_ID, WORKER_CHANNEL_ID),
				0, false,
			)
			notification.addAction(
				R.drawable.ic_settings,
				applicationContext.getString(R.string.notifications_settings),
				actionIntent,
			)
		}
		return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			ForegroundInfo(WORKER_NOTIFICATION_ID, notification.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
		} else {
			ForegroundInfo(WORKER_NOTIFICATION_ID, notification.build())
		}
	}

	companion object {

		private const val TAG = "cleanup"
		private const val WORKER_CHANNEL_ID = "storage_cleanup"
		private const val WORKER_NOTIFICATION_ID = 32
		private const val SETTINGS_ACTION_CODE = 6

		suspend fun enqueue(context: Context) {
			val request = OneTimeWorkRequestBuilder<LocalStorageCleanupWorker>()
				.addTag(TAG)
				.setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.MINUTES)
				.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
				.build()
			WorkManager.getInstance(context).enqueueUniqueWork(TAG, ExistingWorkPolicy.KEEP, request).await()
		}
	}
}
