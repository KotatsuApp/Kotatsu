package org.koitharu.kotatsu.local.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import coil.ImageLoader
import coil.request.ImageRequest
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.CoroutineIntentService
import org.koitharu.kotatsu.core.model.parcelable.ParcelableManga
import org.koitharu.kotatsu.details.ui.DetailsActivity
import org.koitharu.kotatsu.download.ui.service.DownloadService
import org.koitharu.kotatsu.local.domain.importer.MangaImporter
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.utils.PendingIntentCompat
import org.koitharu.kotatsu.utils.ext.asArrayList
import org.koitharu.kotatsu.utils.ext.getDisplayMessage
import org.koitharu.kotatsu.utils.ext.printStackTraceDebug
import org.koitharu.kotatsu.utils.ext.referer
import org.koitharu.kotatsu.utils.ext.report
import org.koitharu.kotatsu.utils.ext.toBitmapOrNull
import javax.inject.Inject

@AndroidEntryPoint
class ImportService : CoroutineIntentService() {

	@Inject
	lateinit var importerFactory: MangaImporter.Factory

	@Inject
	lateinit var coil: ImageLoader

	private lateinit var notificationManager: NotificationManager

	override fun onCreate() {
		super.onCreate()
		isRunning = true
		notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
	}

	override fun onDestroy() {
		isRunning = false
		super.onDestroy()
	}

	override suspend fun processIntent(startId: Int, intent: Intent) {
		val uris = intent.getParcelableArrayListExtra<Uri>(EXTRA_URIS)
		if (uris.isNullOrEmpty()) {
			return
		}
		startForeground()
		for (uri in uris) {
			try {
				val manga = importImpl(uri)
				showNotification(uri, manga, null)
				sendBroadcast(manga)
			} catch (e: CancellationException) {
				throw e
			} catch (e: Throwable) {
				e.printStackTraceDebug()
				showNotification(uri, null, e)
			}
		}
		ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
	}

	override fun onError(startId: Int, error: Throwable) {
		error.report()
	}

	private suspend fun importImpl(uri: Uri): Manga {
		val importer = importerFactory.create(uri)
		return importer.import(uri)
	}

	private fun sendBroadcast(manga: Manga) {
		sendBroadcast(
			Intent(DownloadService.ACTION_DOWNLOAD_COMPLETE)
				.putExtra(DownloadService.EXTRA_MANGA, ParcelableManga(manga, withChapters = false)),
		)
	}

	private suspend fun showNotification(uri: Uri, manga: Manga?, error: Throwable?) {
		val notification = NotificationCompat.Builder(this, CHANNEL_ID)
			.setPriority(NotificationCompat.PRIORITY_DEFAULT)
			.setDefaults(0)
			.setColor(ContextCompat.getColor(this, R.color.blue_primary_dark))
			.setSilent(true)
		if (manga != null) {
			notification.setLargeIcon(
				coil.execute(
					ImageRequest.Builder(applicationContext)
						.data(manga.coverUrl)
						.tag(manga.source)
						.referer(manga.publicUrl)
						.build(),
				).toBitmapOrNull(),
			)
			notification.setSubText(manga.title)
			val intent = DetailsActivity.newIntent(applicationContext, manga)
			notification.setContentIntent(
				PendingIntent.getActivity(
					applicationContext,
					manga.id.toInt(),
					intent,
					PendingIntent.FLAG_UPDATE_CURRENT or PendingIntentCompat.FLAG_IMMUTABLE,
				),
			).setAutoCancel(true)
				.setVisibility(
					if (manga.isNsfw) {
						NotificationCompat.VISIBILITY_SECRET
					} else NotificationCompat.VISIBILITY_PUBLIC,
				)
		}
		if (error != null) {
			notification.setContentTitle(getString(R.string.error_occurred))
				.setContentText(error.getDisplayMessage(resources))
				.setSmallIcon(android.R.drawable.stat_notify_error)
		} else {
			notification.setContentTitle(getString(R.string.import_completed))
				.setContentText(getString(R.string.import_completed_hint))
				.setSmallIcon(R.drawable.ic_stat_done)
			NotificationCompat.BigTextStyle(notification)
				.bigText(getString(R.string.import_completed_hint))
		}

		notificationManager.notify(uri.hashCode(), notification.build())
	}

	private fun startForeground() {
		val title = getString(R.string.importing_manga)
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
			.setSmallIcon(android.R.drawable.stat_sys_download)
			.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
			.setOngoing(true)
			.build()
		startForeground(NOTIFICATION_ID, notification)
	}

	companion object {

		var isRunning: Boolean = false
			private set

		private const val CHANNEL_ID = "importing"
		private const val NOTIFICATION_ID = 22

		private const val EXTRA_URIS = "uris"

		fun start(context: Context, uris: Collection<Uri>) {
			if (uris.isEmpty()) {
				return
			}
			val intent = Intent(context, ImportService::class.java)
			intent.putParcelableArrayListExtra(EXTRA_URIS, uris.asArrayList())
			ContextCompat.startForegroundService(context, intent)
			Toast.makeText(context, R.string.import_will_start_soon, Toast.LENGTH_LONG).show()
		}
	}
}
