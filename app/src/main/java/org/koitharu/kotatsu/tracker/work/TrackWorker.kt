package org.koitharu.kotatsu.tracker.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.work.*
import coil.ImageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaChapter
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.details.ui.DetailsActivity
import org.koitharu.kotatsu.tracker.domain.TrackingRepository
import org.koitharu.kotatsu.utils.ext.mangaRepositoryOf
import org.koitharu.kotatsu.utils.ext.toBitmapOrNull
import org.koitharu.kotatsu.utils.ext.toUriOrNull
import org.koitharu.kotatsu.utils.progress.Progress
import java.util.concurrent.TimeUnit

class TrackWorker(context: Context, workerParams: WorkerParameters) :
	CoroutineWorker(context, workerParams), KoinComponent {

	private val notificationManager by lazy {
		applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
	}

	private val coil by inject<ImageLoader>()
	private val repository by inject<TrackingRepository>()
	private val settings by inject<AppSettings>()

	override suspend fun doWork(): Result {
		val trackSources = settings.trackSources
		if (trackSources.isEmpty()) {
			return Result.success()
		}
		val tracks = repository.getAllTracks(
			useFavourites = AppSettings.TRACK_FAVOURITES in trackSources,
			useHistory = AppSettings.TRACK_HISTORY in trackSources
		)
		if (tracks.isEmpty()) {
			return Result.success()
		}
		if (tracks.size >= FOREGROUND_TRACKERS_THRESHOLD) {
			setForeground(createForegroundInfo())
		}
		var success = 0
		val workData = Data.Builder()
			.putInt(DATA_TOTAL, tracks.size)
		for ((index, track) in tracks.withIndex()) {
			val details = runCatching {
				mangaRepositoryOf(track.manga.source).getDetails(track.manga)
			}.getOrNull()
			workData.putInt(DATA_PROGRESS, index)
			setProgress(workData.build())
			val chapters = details?.chapters ?: continue
			when {
				track.knownChaptersCount == -1 -> { //first check
					repository.storeTrackResult(
						mangaId = track.manga.id,
						knownChaptersCount = chapters.size,
						lastChapterId = chapters.lastOrNull()?.id ?: 0L,
						previousTrackChapterId = 0L,
						newChapters = emptyList()
					)
				}
				track.knownChaptersCount == 0 && track.lastChapterId == 0L -> { //manga was empty on last check
					repository.storeTrackResult(
						mangaId = track.manga.id,
						knownChaptersCount = track.knownChaptersCount,
						lastChapterId = 0L,
						previousTrackChapterId = track.lastNotifiedChapterId,
						newChapters = chapters
					)
					showNotification(track.manga, chapters)
				}
				chapters.size == track.knownChaptersCount -> {
					if (chapters.lastOrNull()?.id == track.lastChapterId) {
						// manga was not updated. skip
					} else {
						// number of chapters still the same, bu last chapter changed.
						// maybe some chapters are removed. we need to find last known chapter
						val knownChapter = chapters.indexOfLast { it.id == track.lastChapterId }
						if (knownChapter == -1) {
							// confuse. reset anything
							repository.storeTrackResult(
								mangaId = track.manga.id,
								knownChaptersCount = chapters.size,
								lastChapterId = chapters.lastOrNull()?.id ?: 0L,
								previousTrackChapterId = track.lastNotifiedChapterId,
								newChapters = emptyList()
							)
						} else {
							val newChapters = chapters.takeLast(chapters.size - knownChapter + 1)
							repository.storeTrackResult(
								mangaId = track.manga.id,
								knownChaptersCount = knownChapter + 1,
								lastChapterId = track.lastChapterId,
								previousTrackChapterId = track.lastNotifiedChapterId,
								newChapters = newChapters
							)
							showNotification(
								track.manga,
								newChapters.takeLastWhile { x -> x.id != track.lastNotifiedChapterId }
							)
						}
					}
				}
				else -> {
					val newChapters = chapters.takeLast(chapters.size - track.knownChaptersCount)
					repository.storeTrackResult(
						mangaId = track.manga.id,
						knownChaptersCount = track.knownChaptersCount,
						lastChapterId = track.lastChapterId,
						previousTrackChapterId = track.lastNotifiedChapterId,
						newChapters = newChapters
					)
					showNotification(
						track.manga,
						newChapters.takeLastWhile { x -> x.id != track.lastNotifiedChapterId }
					)
				}
			}
			success++
		}
		repository.cleanup()
		return if (success == 0) {
			Result.retry()
		} else {
			Result.success()
		}
	}

	private suspend fun showNotification(manga: Manga, newChapters: List<MangaChapter>) {
		if (newChapters.isEmpty() || !settings.trackerNotifications) {
			return
		}
		val id = manga.url.hashCode()
		val colorPrimary = ContextCompat.getColor(applicationContext, R.color.blue_primary)
		val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
		val summary = applicationContext.resources.getQuantityString(
			R.plurals.new_chapters,
			newChapters.size, newChapters.size
		)
		with(builder) {
			setContentText(summary)
			setContentTitle(manga.title)
			setNumber(newChapters.size)
			setLargeIcon(
				coil.execute(
					ImageRequest.Builder(applicationContext)
						.data(manga.coverUrl)
						.build()
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
					applicationContext, id,
					intent, PendingIntent.FLAG_UPDATE_CURRENT
				)
			)
			setAutoCancel(true)
			color = colorPrimary
			setShortcutId(manga.id.toString())
			priority = NotificationCompat.PRIORITY_DEFAULT
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
				builder.setSound(settings.notificationSound.toUriOrNull())
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

	private fun createForegroundInfo(): ForegroundInfo {
		val title = applicationContext.getString(R.string.new_chapters_checking)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val channel =
				NotificationChannel(WORKER_CHANNEL_ID, title, NotificationManager.IMPORTANCE_NONE)
			channel.setShowBadge(false)
			channel.enableVibration(false)
			channel.setSound(null, null)
			channel.enableLights(false)
			notificationManager.createNotificationChannel(channel)
		}

		val notification = NotificationCompat.Builder(applicationContext, WORKER_CHANNEL_ID)
			.setContentTitle(title)
			.setPriority(NotificationCompat.PRIORITY_MIN)
			.setDefaults(0)
			.setColor(ContextCompat.getColor(applicationContext, R.color.blue_primary_dark))
			.setSilent(true)
			.setProgress(0, 0, true)
			.setSmallIcon(android.R.drawable.stat_notify_sync)
			.setOngoing(true)
			.build()

		return ForegroundInfo(WORKER_NOTIFICATION_ID, notification)
	}

	companion object {

		const val CHANNEL_ID = "tracking"
		private const val WORKER_CHANNEL_ID = "track_worker"
		private const val WORKER_NOTIFICATION_ID = 35
		private const val DATA_PROGRESS = "progress"
		private const val DATA_TOTAL = "total"
		private const val TAG = "tracking"
		private const val FOREGROUND_TRACKERS_THRESHOLD = 4

		@RequiresApi(Build.VERSION_CODES.O)
		private fun createNotificationChannel(context: Context) {
			val manager =
				context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
			if (manager.getNotificationChannel(CHANNEL_ID) == null) {
				val channel = NotificationChannel(
					CHANNEL_ID,
					context.getString(R.string.new_chapters),
					NotificationManager.IMPORTANCE_DEFAULT
				)
				channel.setShowBadge(true)
				channel.lightColor = ContextCompat.getColor(context, R.color.blue_primary_dark)
				channel.enableLights(true)
				manager.createNotificationChannel(channel)
			}
		}

		fun setup(context: Context) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				createNotificationChannel(context)
			}
			val constraints = Constraints.Builder()
				.setRequiredNetworkType(NetworkType.CONNECTED)
				.build()
			val request = PeriodicWorkRequestBuilder<TrackWorker>(4, TimeUnit.HOURS)
				.setConstraints(constraints)
				.addTag(TAG)
				.setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.MINUTES)
				.build()
			WorkManager.getInstance(context)
				.enqueueUniquePeriodicWork(TAG, ExistingPeriodicWorkPolicy.KEEP, request)
		}

		fun startNow(context: Context) {
			val constraints = Constraints.Builder()
				.setRequiredNetworkType(NetworkType.CONNECTED)
				.build()
			val request = OneTimeWorkRequestBuilder<TrackWorker>()
				.setConstraints(constraints)
				.addTag(TAG)
				.build()
			WorkManager.getInstance(context)
				.enqueue(request)
		}

		fun getProgressLiveData(context: Context): LiveData<Progress?> {
			return WorkManager.getInstance(context)
				.getWorkInfosByTagLiveData(TAG)
				.map { list ->
					list.find { work ->
						work.state == WorkInfo.State.RUNNING
					}?.let { workInfo ->
						Progress(
							value = workInfo.progress.getInt(DATA_PROGRESS, 0),
							total = workInfo.progress.getInt(DATA_TOTAL, -1)
						).takeUnless { it.isIndeterminate }
					}
				}
		}
	}
}