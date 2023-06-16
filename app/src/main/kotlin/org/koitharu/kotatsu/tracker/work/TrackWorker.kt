package org.koitharu.kotatsu.tracker.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC
import androidx.core.app.NotificationCompat.VISIBILITY_SECRET
import androidx.core.app.PendingIntentCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.lifecycle.asFlow
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import androidx.work.WorkerParameters
import androidx.work.await
import coil.ImageLoader
import coil.request.ImageRequest
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.logs.FileLogger
import org.koitharu.kotatsu.core.logs.TrackerLogger
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.util.WorkManagerHelper
import org.koitharu.kotatsu.core.util.ext.toBitmapOrNull
import org.koitharu.kotatsu.core.util.ext.trySetForeground
import org.koitharu.kotatsu.details.ui.DetailsActivity
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import org.koitharu.kotatsu.settings.work.PeriodicWorkScheduler
import org.koitharu.kotatsu.tracker.domain.Tracker
import org.koitharu.kotatsu.tracker.domain.model.MangaUpdates
import java.util.concurrent.TimeUnit

@HiltWorker
class TrackWorker @AssistedInject constructor(
	@Assisted context: Context,
	@Assisted workerParams: WorkerParameters,
	private val coil: ImageLoader,
	private val settings: AppSettings,
	private val tracker: Tracker,
	@TrackerLogger private val logger: FileLogger,
) : CoroutineWorker(context, workerParams) {

	private val notificationManager by lazy {
		applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
	}

	override suspend fun doWork(): Result {
		trySetForeground()
		logger.log("doWork()")
		try {
			return doWorkImpl()
		} catch (e: Throwable) {
			logger.log("fatal", e)
			throw e
		} finally {
			withContext(NonCancellable) {
				logger.flush()
				notificationManager.cancel(WORKER_NOTIFICATION_ID)
			}
		}
	}

	private suspend fun doWorkImpl(): Result {
		if (!settings.isTrackerEnabled) {
			return Result.success(workDataOf(0, 0))
		}
		val tracks = tracker.getAllTracks()
		logger.log("Total ${tracks.size} tracks")
		if (tracks.isEmpty()) {
			return Result.success(workDataOf(0, 0))
		}

		val results = checkUpdatesAsync(tracks)
		tracker.gc()

		var success = 0
		var failed = 0
		results.forEach { x ->
			if (x == null) {
				failed++
			} else {
				success++
			}
		}
		logger.log("Result: success: $success, failed: $failed")
		val resultData = workDataOf(success, failed)
		return if (success == 0 && failed != 0) {
			Result.failure(resultData)
		} else {
			Result.success(resultData)
		}
	}

	private suspend fun checkUpdatesAsync(tracks: List<TrackingItem>): List<MangaUpdates?> {
		val dispatcher = Dispatchers.Default.limitedParallelism(MAX_PARALLELISM)
		return supervisorScope {
			tracks.map { (track, channelId) ->
				async(dispatcher) {
					runCatchingCancellable {
						tracker.fetchUpdates(track, commit = true)
					}.onFailure {
						logger.log("checkUpdatesAsync", it)
					}.onSuccess { updates ->
						if (updates.isValid && updates.isNotEmpty()) {
							showNotification(
								manga = updates.manga,
								channelId = channelId,
								newChapters = updates.newChapters,
							)
						}
					}.getOrNull()
				}
			}.awaitAll()
		}
	}

	private suspend fun showNotification(manga: Manga, channelId: String?, newChapters: List<MangaChapter>) {
		if (newChapters.isEmpty() || channelId == null) {
			return
		}
		val id = manga.url.hashCode()
		val colorPrimary = ContextCompat.getColor(applicationContext, R.color.blue_primary)
		val builder = NotificationCompat.Builder(applicationContext, channelId)
		val summary = applicationContext.resources.getQuantityString(
			R.plurals.new_chapters,
			newChapters.size,
			newChapters.size,
		)
		with(builder) {
			setContentText(summary)
			setContentTitle(manga.title)
			setNumber(newChapters.size)
			setLargeIcon(
				coil.execute(
					ImageRequest.Builder(applicationContext)
						.data(manga.coverUrl)
						.tag(manga.source)
						.build(),
				).toBitmapOrNull(),
			)
			setSmallIcon(R.drawable.ic_stat_book_plus)
			val style = NotificationCompat.InboxStyle(this)
			for (chapter in newChapters) {
				style.addLine(chapter.name)
			}
			style.setSummaryText(manga.title)
			style.setBigContentTitle(summary)
			setStyle(style)
			val intent = DetailsActivity.newIntent(applicationContext, manga)
			setContentIntent(
				PendingIntentCompat.getActivity(
					applicationContext,
					id,
					intent,
					PendingIntent.FLAG_UPDATE_CURRENT,
					false,
				),
			)
			setAutoCancel(true)
			setCategory(NotificationCompat.CATEGORY_PROMO)
			setVisibility(if (manga.isNsfw) VISIBILITY_SECRET else VISIBILITY_PUBLIC)
			setShortcutId(manga.id.toString())
			priority = NotificationCompat.PRIORITY_DEFAULT
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
				builder.setSound(settings.notificationSound)
				var defaults = if (settings.notificationLight) {
					setLights(colorPrimary, 1000, 5000)
					NotificationCompat.DEFAULT_LIGHTS
				} else 0
				if (settings.notificationVibrate) {
					builder.setVibrate(longArrayOf(500, 500, 500, 500))
					defaults = defaults or NotificationCompat.DEFAULT_VIBRATE
				}
				builder.setDefaults(defaults)
			}
		}
		notificationManager.notify(TAG, id, builder.build())
	}

	override suspend fun getForegroundInfo(): ForegroundInfo {
		val title = applicationContext.getString(R.string.check_for_new_chapters)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val channel = NotificationChannel(
				WORKER_CHANNEL_ID,
				title,
				NotificationManager.IMPORTANCE_LOW,
			)
			channel.setShowBadge(false)
			channel.enableVibration(false)
			channel.setSound(null, null)
			channel.enableLights(false)
			notificationManager.createNotificationChannel(channel)
		}
		val notification = NotificationCompat.Builder(applicationContext, WORKER_CHANNEL_ID)
			.setContentTitle(title)
			.setPriority(NotificationCompat.PRIORITY_MIN)
			.setCategory(NotificationCompat.CATEGORY_SERVICE)
			.setDefaults(0)
			.setOngoing(true)
			.setSilent(true)
			.setProgress(0, 0, true)
			.setSmallIcon(android.R.drawable.stat_notify_sync)
			.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_DEFERRED)
			.build()
		return ForegroundInfo(WORKER_NOTIFICATION_ID, notification)
	}

	private fun workDataOf(success: Int, failed: Int): Data {
		return Data.Builder()
			.putInt(DATA_KEY_SUCCESS, success)
			.putInt(DATA_KEY_FAILED, failed)
			.build()
	}

	companion object : PeriodicWorkScheduler {

		private const val WORKER_CHANNEL_ID = "track_worker"
		private const val WORKER_NOTIFICATION_ID = 35
		private const val TAG = "tracking"
		private const val TAG_ONESHOT = "tracking_oneshot"
		private const val MAX_PARALLELISM = 4
		private const val DATA_KEY_SUCCESS = "success"
		private const val DATA_KEY_FAILED = "failed"

		override suspend fun schedule(context: Context) {
			val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
			val request = PeriodicWorkRequestBuilder<TrackWorker>(4, TimeUnit.HOURS)
				.setConstraints(constraints)
				.addTag(TAG)
				.setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.MINUTES)
				.build()
			WorkManager.getInstance(context)
				.enqueueUniquePeriodicWork(TAG, ExistingPeriodicWorkPolicy.KEEP, request)
				.await()
		}

		override suspend fun unschedule(context: Context) {
			WorkManager.getInstance(context)
				.cancelUniqueWork(TAG)
				.await()
		}

		override suspend fun isScheduled(context: Context): Boolean {
			return WorkManagerHelper(WorkManager.getInstance(context))
				.getUniqueWorkInfoByName(TAG)
				.any { !it.state.isFinished }
		}

		fun startNow(context: Context) {
			val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
			val request = OneTimeWorkRequestBuilder<TrackWorker>()
				.setConstraints(constraints)
				.addTag(TAG_ONESHOT)
				.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
				.build()
			WorkManager.getInstance(context).enqueue(request)
		}

		fun observeIsRunning(context: Context): Flow<Boolean> {
			val query = WorkQuery.Builder.fromTags(listOf(TAG, TAG_ONESHOT)).build()
			return WorkManager.getInstance(context).getWorkInfosLiveData(query)
				.asFlow()
				.map { works ->
					works.any { x -> x.state == WorkInfo.State.RUNNING }
				}
		}
	}
}
