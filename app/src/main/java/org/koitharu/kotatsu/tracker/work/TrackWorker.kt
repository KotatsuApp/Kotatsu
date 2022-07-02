package org.koitharu.kotatsu.tracker.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC
import androidx.core.app.NotificationCompat.VISIBILITY_SECRET
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.work.*
import coil.ImageLoader
import coil.request.ImageRequest
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.details.ui.DetailsActivity
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.tracker.domain.Tracker
import org.koitharu.kotatsu.tracker.domain.model.MangaUpdates
import org.koitharu.kotatsu.utils.PendingIntentCompat
import org.koitharu.kotatsu.utils.ext.referer
import org.koitharu.kotatsu.utils.ext.toBitmapOrNull
import org.koitharu.kotatsu.utils.ext.trySetForeground

class TrackWorker(context: Context, workerParams: WorkerParameters) :
	CoroutineWorker(context, workerParams),
	KoinComponent {

	private val notificationManager by lazy {
		applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
	}

	private val coil by inject<ImageLoader>()
	private val settings by inject<AppSettings>()
	private val tracker by inject<Tracker>()

	override suspend fun doWork(): Result {
		if (!settings.isTrackerEnabled) {
			return Result.success(workDataOf(0, 0))
		}
		if (TAG in tags) { // not expedited
			trySetForeground()
		}
		val tracks = tracker.getAllTracks()
		if (tracks.isEmpty()) {
			return Result.success(workDataOf(0, 0))
		}

		val updates = checkUpdatesAsync(tracks)
		val results = updates.awaitAll()
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
		val resultData = workDataOf(success, failed)
		return if (success == 0 && failed != 0) {
			Result.failure(resultData)
		} else {
			Result.success(resultData)
		}
	}

	private suspend fun checkUpdatesAsync(tracks: List<TrackingItem>): List<Deferred<MangaUpdates?>> {
		val dispatcher = Dispatchers.Default.limitedParallelism(MAX_PARALLELISM)
		val deferredList = coroutineScope {
			tracks.map { (track, channelId) ->
				async(dispatcher) {
					runCatching {
						tracker.fetchUpdates(track, commit = true)
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
			}
		}
		return deferredList
	}

	private suspend fun showNotification(manga: Manga, channelId: String?, newChapters: List<MangaChapter>) {
		if (newChapters.isEmpty() || channelId == null) {
			return
		}
		val id = manga.url.hashCode()
		val colorPrimary = ContextCompat.getColor(applicationContext, R.color.blue_primary)
		val builder = NotificationCompat.Builder(applicationContext, channelId)
		val summary = applicationContext.resources.getQuantityString(
			R.plurals.new_chapters, newChapters.size, newChapters.size
		)
		with(builder) {
			setContentText(summary)
			setContentTitle(manga.title)
			setNumber(newChapters.size)
			setLargeIcon(
				coil.execute(
					ImageRequest.Builder(applicationContext).data(manga.coverUrl).referer(manga.publicUrl).build()
				).toBitmapOrNull()
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
				PendingIntent.getActivity(
					applicationContext,
					id,
					intent,
					PendingIntent.FLAG_UPDATE_CURRENT or PendingIntentCompat.FLAG_IMMUTABLE
				)
			)
			setAutoCancel(true)
			setVisibility(if (manga.isNsfw) VISIBILITY_SECRET else VISIBILITY_PUBLIC)
			color = colorPrimary
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
		withContext(Dispatchers.Main) {
			notificationManager.notify(TAG, id, builder.build())
		}
	}

	override suspend fun getForegroundInfo(): ForegroundInfo {
		val title = applicationContext.getString(R.string.check_for_new_chapters)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val channel = NotificationChannel(
				WORKER_CHANNEL_ID, title, NotificationManager.IMPORTANCE_LOW
			)
			channel.setShowBadge(false)
			channel.enableVibration(false)
			channel.setSound(null, null)
			channel.enableLights(false)
			notificationManager.createNotificationChannel(channel)
		}

		val notification = NotificationCompat.Builder(applicationContext, WORKER_CHANNEL_ID).setContentTitle(title)
			.setPriority(NotificationCompat.PRIORITY_MIN).setDefaults(0)
			.setColor(ContextCompat.getColor(applicationContext, R.color.blue_primary_dark)).setSilent(true)
			.setProgress(0, 0, true).setSmallIcon(android.R.drawable.stat_notify_sync)
			.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_DEFERRED).setOngoing(true).build()

		return ForegroundInfo(WORKER_NOTIFICATION_ID, notification)
	}

	private fun workDataOf(success: Int, failed: Int): Data {
		return Data.Builder()
			.putInt(DATA_KEY_SUCCESS, success)
			.putInt(DATA_KEY_FAILED, failed)
			.build()
	}

	companion object {

		private const val WORKER_CHANNEL_ID = "track_worker"
		private const val WORKER_NOTIFICATION_ID = 35
		private const val TAG = "tracking"
		private const val TAG_ONESHOT = "tracking_oneshot"
		private const val MAX_PARALLELISM = 4
		private const val DATA_KEY_SUCCESS = "success"
		private const val DATA_KEY_FAILED = "failed"

		fun setup(context: Context) {
			val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
			val request =
				PeriodicWorkRequestBuilder<TrackWorker>(4, TimeUnit.HOURS).setConstraints(constraints).addTag(TAG)
					.setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.MINUTES).build()
			WorkManager.getInstance(context).enqueueUniquePeriodicWork(TAG, ExistingPeriodicWorkPolicy.KEEP, request)
		}

		fun startNow(context: Context) {
			val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
			val request = OneTimeWorkRequestBuilder<TrackWorker>().setConstraints(constraints).addTag(TAG_ONESHOT)
				.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST).build()
			WorkManager.getInstance(context).enqueue(request)
		}

		fun getIsRunningLiveData(context: Context): LiveData<Boolean> {
			val query = WorkQuery.Builder.fromTags(listOf(TAG, TAG_ONESHOT)).build()
			return WorkManager.getInstance(context).getWorkInfosLiveData(query).map { works ->
				works.any { x -> x.state == WorkInfo.State.RUNNING }
			}
		}
	}
}