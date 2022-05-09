package org.koitharu.kotatsu.tracker.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
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
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.details.ui.DetailsActivity
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.tracker.domain.TrackingRepository
import org.koitharu.kotatsu.utils.PendingIntentCompat
import org.koitharu.kotatsu.utils.ext.referer
import org.koitharu.kotatsu.utils.ext.toBitmapOrNull
import org.koitharu.kotatsu.utils.ext.trySetForeground
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
	private val channels by inject<TrackerNotificationChannels>()

	override suspend fun doWork(): Result {
		if (!settings.isTrackerEnabled) {
			return Result.success()
		}
		if (TAG in tags) { // not expedited
			trySetForeground()
		}
		val tracks = getAllTracks()

		var success = 0
		val workData = Data.Builder()
			.putInt(DATA_TOTAL, tracks.size)
		for ((index, item) in tracks.withIndex()) {
			val (track, channelId) = item
			val details = runCatching {
				MangaRepository(track.manga.source).getDetails(track.manga)
			}.getOrNull()
			workData.putInt(DATA_PROGRESS, index)
			setProgress(workData.build())
			val chapters = details?.chapters ?: continue
			when {
				track.knownChaptersCount == -1 -> { // first check
					repository.storeTrackResult(
						mangaId = track.manga.id,
						knownChaptersCount = chapters.size,
						lastChapterId = chapters.lastOrNull()?.id ?: 0L,
						previousTrackChapterId = 0L,
						newChapters = emptyList()
					)
				}
				track.knownChaptersCount == 0 && track.lastChapterId == 0L -> { // manga was empty on last check
					repository.storeTrackResult(
						mangaId = track.manga.id,
						knownChaptersCount = 0,
						lastChapterId = 0L,
						previousTrackChapterId = track.lastNotifiedChapterId,
						newChapters = chapters
					)
					showNotification(details, channelId, chapters)
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
								details,
								channelId,
								newChapters.takeLastWhile { x -> x.id != track.lastNotifiedChapterId },
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
						newChapters = newChapters,
					)
					showNotification(
						manga = track.manga,
						channelId = channelId,
						newChapters = newChapters.takeLastWhile { x -> x.id != track.lastNotifiedChapterId },
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

	private suspend fun getAllTracks(): List<TrackingItem> {
		val sources = settings.trackSources
		if (sources.isEmpty()) {
			return emptyList()
		}
		val knownIds = HashSet<Manga>()
		val result = ArrayList<TrackingItem>()
		// Favourites
		if (AppSettings.TRACK_FAVOURITES in sources) {
			val favourites = repository.getFavouritesManga()
			channels.updateChannels(favourites.keys)
			for ((category, mangaList) in favourites) {
				if (!category.isTrackingEnabled || mangaList.isEmpty()) {
					continue
				}
				val categoryTracks = repository.getTracks(mangaList)
				val channelId = if (channels.isFavouriteNotificationsEnabled(category)) {
					channels.getFavouritesChannelId(category.id)
				} else {
					null
				}
				for (track in categoryTracks) {
					if (knownIds.add(track.manga)) {
						result.add(TrackingItem(track, channelId))
					}
				}
			}
		}
		// History
		if (AppSettings.TRACK_HISTORY in sources) {
			val history = repository.getHistoryManga()
			val historyTracks = repository.getTracks(history)
			val channelId = if (channels.isHistoryNotificationsEnabled()) {
				channels.getHistoryChannelId()
			} else {
				null
			}
			for (track in historyTracks) {
				if (knownIds.add(track.manga)) {
					result.add(TrackingItem(track, channelId))
				}
			}
		}
		result.trimToSize()
		return result
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
						.referer(manga.publicUrl)
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
					intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntentCompat.FLAG_IMMUTABLE
				)
			)
			setAutoCancel(true)
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
				WORKER_CHANNEL_ID,
				title,
				NotificationManager.IMPORTANCE_LOW
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
			.setDefaults(0)
			.setColor(ContextCompat.getColor(applicationContext, R.color.blue_primary_dark))
			.setSilent(true)
			.setProgress(0, 0, true)
			.setSmallIcon(android.R.drawable.stat_notify_sync)
			.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_DEFERRED)
			.setOngoing(true)
			.build()

		return ForegroundInfo(WORKER_NOTIFICATION_ID, notification)
	}

	companion object {

		private const val WORKER_CHANNEL_ID = "track_worker"
		private const val WORKER_NOTIFICATION_ID = 35
		private const val DATA_PROGRESS = "progress"
		private const val DATA_TOTAL = "total"
		private const val TAG = "tracking"
		private const val TAG_ONESHOT = "tracking_oneshot"

		fun setup(context: Context) {
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
				.addTag(TAG_ONESHOT)
				.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
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