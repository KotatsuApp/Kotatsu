package org.koitharu.kotatsu.ui.tracker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import coil.Coil
import coil.api.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaChapter
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.domain.MangaProviderFactory
import org.koitharu.kotatsu.domain.tracking.TrackingRepository
import org.koitharu.kotatsu.ui.common.BaseJobService
import org.koitharu.kotatsu.ui.details.MangaDetailsActivity
import org.koitharu.kotatsu.utils.ext.safe
import java.util.concurrent.TimeUnit

class TrackerJobService : BaseJobService() {

	private val notificationManager by lazy(LazyThreadSafetyMode.NONE) {
		getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
	}

	private val settings by inject<AppSettings>()

	override suspend fun doWork(params: JobParameters) {
		withContext(Dispatchers.IO) {
			val repo = TrackingRepository()
			val tracks = repo.getAllTracks()
			if (tracks.isEmpty()) {
				return@withContext
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
							newChapters = 0
						)
					}
					track.knownChaptersCount == 0 && track.lastChapterId == 0L -> { //manga was empty on last check
						repo.storeTrackResult(
							mangaId = track.manga.id,
							knownChaptersCount = track.knownChaptersCount,
							lastChapterId = 0L,
							newChapters = chapters.size
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
									newChapters = 0
								)
							} else {
								val newChapters = chapters.size - knownChapter + 1
								repo.storeTrackResult(
									mangaId = track.manga.id,
									knownChaptersCount = knownChapter + 1,
									lastChapterId = track.lastChapterId,
									newChapters = newChapters
								)
								showNotification(track.manga, chapters.takeLast(newChapters))
							}
						}
					}
					else -> {
						val newChapters = chapters.size - track.knownChaptersCount
						repo.storeTrackResult(
							mangaId = track.manga.id,
							knownChaptersCount = track.knownChaptersCount,
							lastChapterId = track.lastChapterId,
							newChapters = newChapters
						)
						showNotification(track.manga, chapters.takeLast(newChapters))
					}
				}
				success++
			}
			if (success == 0) {
				throw RuntimeException("Cannot check any manga updates")
			}
		}
	}

	private suspend fun showNotification(manga: Manga, newChapters: List<MangaChapter>) {
		if (newChapters.isEmpty() || !settings.trackerNotifications) {
			return
		}
		val id = manga.url.hashCode()
		val colorPrimary = ContextCompat.getColor(this@TrackerJobService, R.color.blue_primary)
		val builder = NotificationCompat.Builder(this, CHANNEL_ID)
		with(builder) {
			setContentText(resources.getQuantityString(R.plurals.new_chapters,
				newChapters.size, newChapters.size))
			setContentText(manga.title)
			setNumber(newChapters.size)
			setLargeIcon(safe {
				Coil.loader().get(manga.coverUrl).toBitmap()
			})
			setSmallIcon(R.drawable.ic_stat_book_plus)
			val style = NotificationCompat.InboxStyle(this)
			for (chapter in newChapters) {
				style.addLine(chapter.name)
			}
			style.setSummaryText(manga.title)
			setStyle(style)
			val intent = MangaDetailsActivity.newIntent(this@TrackerJobService, manga)
			setContentIntent(PendingIntent.getActivity(this@TrackerJobService, id,
				intent, PendingIntent.FLAG_UPDATE_CURRENT))
			setAutoCancel(true)
			color = colorPrimary
			setLights(colorPrimary, 1000, 5000)
			setPriority(NotificationCompat.PRIORITY_DEFAULT)
		}
		withContext(Dispatchers.Main) {
			notificationManager.notify(TAG, id, builder.build())
		}
	}

	companion object {

		private const val JOB_ID = 7
		private const val CHANNEL_ID = "tracking"
		private const val TAG = "tracking"

		@RequiresApi(Build.VERSION_CODES.O)
		private fun createNotificationChannel(context: Context) {
			val manager =
				context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
			if (manager.getNotificationChannel(CHANNEL_ID) == null) {
				val channel = NotificationChannel(CHANNEL_ID,
					context.getString(R.string.new_chapters),
					NotificationManager.IMPORTANCE_DEFAULT)
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
			val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
			// if (scheduler.allPendingJobs != null) {
			// 	return
			// }
			val jobInfo =
				JobInfo.Builder(JOB_ID, ComponentName(context, TrackerJobService::class.java))
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
				jobInfo.setRequiredNetworkType(JobInfo.NETWORK_TYPE_NOT_ROAMING)
			} else {
				jobInfo.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
			}
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				jobInfo.setRequiresBatteryNotLow(true)
			}
			jobInfo.setRequiresDeviceIdle(true)
			jobInfo.setPersisted(true)
			jobInfo.setPeriodic(TimeUnit.HOURS.toMillis(6))
			scheduler.schedule(jobInfo.build())
		}
	}
}