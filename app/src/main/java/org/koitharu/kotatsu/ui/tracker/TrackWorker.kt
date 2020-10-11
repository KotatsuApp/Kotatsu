package org.koitharu.kotatsu.ui.tracker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import coil.Coil
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaChapter
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.domain.MangaProviderFactory
import org.koitharu.kotatsu.domain.tracking.TrackingRepository
import org.koitharu.kotatsu.ui.details.MangaDetailsActivity
import org.koitharu.kotatsu.utils.ext.safe
import org.koitharu.kotatsu.utils.ext.toBitmapOrNull
import org.koitharu.kotatsu.utils.ext.toUriOrNull
import java.util.concurrent.TimeUnit

class TrackWorker(context: Context, workerParams: WorkerParameters) :
	CoroutineWorker(context, workerParams), KoinComponent {

	private val notificationManager by lazy(LazyThreadSafetyMode.NONE) {
		applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
	}

	private val settings by inject<AppSettings>()

	override suspend fun doWork(): Result = withContext(Dispatchers.Default) {
		val trackSources = settings.trackSources
		if (trackSources.isEmpty()) {
			return@withContext Result.success()
		}
		val repo = TrackingRepository()
		val tracks = repo.getAllTracks(
			useFavourites = AppSettings.TRACK_FAVOURITES in trackSources,
			useHistory = AppSettings.TRACK_HISTORY in trackSources
		)
		if (tracks.isEmpty()) {
			return@withContext Result.success()
		}
		var success = 0
		for (track in tracks) {
			val details = safe {
				MangaProviderFactory.create(track.manga.source)
					.getDetails(track.manga)
			}
			val chapters = details?.chapters ?: continue
			when {
				track.knownChaptersCount == -1 -> { //first check
					repo.storeTrackResult(
						mangaId = track.manga.id,
						knownChaptersCount = chapters.size,
						lastChapterId = chapters.lastOrNull()?.id ?: 0L,
						previousTrackChapterId = 0L,
						newChapters = emptyList()
					)
				}
				track.knownChaptersCount == 0 && track.lastChapterId == 0L -> { //manga was empty on last check
					repo.storeTrackResult(
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
							repo.storeTrackResult(
								mangaId = track.manga.id,
								knownChaptersCount = chapters.size,
								lastChapterId = chapters.lastOrNull()?.id ?: 0L,
								previousTrackChapterId = track.lastNotifiedChapterId,
								newChapters = emptyList()
							)
						} else {
							val newChapters = chapters.takeLast(chapters.size - knownChapter + 1)
							repo.storeTrackResult(
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
					repo.storeTrackResult(
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
		repo.cleanup()
		if (success == 0) {
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
				Coil.execute(
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
			val intent = MangaDetailsActivity.newIntent(applicationContext, manga)
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

	companion object {

		const val CHANNEL_ID = "tracking"
		private const val TAG = "tracking"

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
				channel.lightColor = ContextCompat.getColor(context, R.color.blue_primary)
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
	}
}