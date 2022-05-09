package org.koitharu.kotatsu.local.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import org.koin.android.ext.android.inject
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.CoroutineIntentService
import org.koitharu.kotatsu.core.model.parcelable.ParcelableManga
import org.koitharu.kotatsu.download.ui.service.DownloadService
import org.koitharu.kotatsu.local.domain.LocalMangaRepository
import org.koitharu.kotatsu.parsers.model.Manga

class LocalChaptersRemoveService : CoroutineIntentService() {

	private val localMangaRepository by inject<LocalMangaRepository>()

	override suspend fun processIntent(intent: Intent?) {
		val manga = intent?.getParcelableExtra<ParcelableManga>(EXTRA_MANGA)?.manga ?: return
		val chaptersIds = intent.getLongArrayExtra(EXTRA_CHAPTERS_IDS)?.toSet() ?: return
		startForeground()
		val mangaWithChapters = localMangaRepository.getDetails(manga)
		localMangaRepository.deleteChapters(mangaWithChapters, chaptersIds)
		sendBroadcast(
			Intent(DownloadService.ACTION_DOWNLOAD_COMPLETE)
				.putExtra(EXTRA_MANGA, ParcelableManga(manga, withChapters = false))
		)
		ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
	}

	private fun startForeground() {
		val title = getString(R.string.local_manga_processing)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
			val channel = NotificationChannel(CHANNEL_ID, title, NotificationManager.IMPORTANCE_LOW)
			channel.setShowBadge(false)
			channel.enableVibration(false)
			channel.setSound(null, null)
			channel.enableLights(false)
			manager.createNotificationChannel(channel)
		}

		val notification = NotificationCompat.Builder(this, CHANNEL_ID)
			.setContentTitle(title)
			.setPriority(NotificationCompat.PRIORITY_MIN)
			.setDefaults(0)
			.setColor(ContextCompat.getColor(this, R.color.blue_primary_dark))
			.setSilent(true)
			.setProgress(0, 0, true)
			.setSmallIcon(android.R.drawable.stat_notify_sync)
			.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_DEFERRED)
			.setOngoing(true)
			.build()
		startForeground(NOTIFICATION_ID, notification)
	}

	companion object {

		private const val CHANNEL_ID = "local_processing"
		private const val NOTIFICATION_ID = 21

		private const val EXTRA_MANGA = "manga"
		private const val EXTRA_CHAPTERS_IDS = "chapters_ids"

		fun start(context: Context, manga: Manga, chaptersIds: Collection<Long>) {
			if (chaptersIds.isEmpty()) {
				return
			}
			val intent = Intent(context, LocalChaptersRemoveService::class.java)
			intent.putExtra(EXTRA_MANGA, ParcelableManga(manga, withChapters = false))
			intent.putExtra(EXTRA_CHAPTERS_IDS, chaptersIds.toLongArray())
			ContextCompat.startForegroundService(context, intent)
		}
	}
}