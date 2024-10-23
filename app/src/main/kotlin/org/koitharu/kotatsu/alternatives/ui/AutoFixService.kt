package org.koitharu.kotatsu.alternatives.ui

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import coil3.ImageLoader
import coil3.request.ImageRequest
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.alternatives.domain.AutoFixUseCase
import org.koitharu.kotatsu.core.ErrorReporterReceiver
import org.koitharu.kotatsu.core.model.getTitle
import org.koitharu.kotatsu.core.ui.CoroutineIntentService
import org.koitharu.kotatsu.core.util.ext.checkNotificationPermission
import org.koitharu.kotatsu.core.util.ext.getDisplayMessage
import org.koitharu.kotatsu.core.util.ext.mangaSourceExtra
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import org.koitharu.kotatsu.core.util.ext.toBitmapOrNull
import org.koitharu.kotatsu.details.ui.DetailsActivity
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import javax.inject.Inject
import com.google.android.material.R as materialR

@AndroidEntryPoint
class AutoFixService : CoroutineIntentService() {

	@Inject
	lateinit var autoFixUseCase: AutoFixUseCase

	@Inject
	lateinit var coil: ImageLoader

	private lateinit var notificationManager: NotificationManagerCompat

	override fun onCreate() {
		super.onCreate()
		notificationManager = NotificationManagerCompat.from(applicationContext)
	}

	override suspend fun processIntent(startId: Int, intent: Intent) {
		val ids = requireNotNull(intent.getLongArrayExtra(DATA_IDS))
		startForeground(startId)
		try {
			for (mangaId in ids) {
				val result = runCatchingCancellable {
					autoFixUseCase.invoke(mangaId)
				}
				if (applicationContext.checkNotificationPermission(CHANNEL_ID)) {
					val notification = buildNotification(result)
					notificationManager.notify(TAG, startId, notification)
				}
			}
		} finally {
			ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
		}
	}

	override fun onError(startId: Int, error: Throwable) {
		if (applicationContext.checkNotificationPermission(CHANNEL_ID)) {
			val notification = runBlocking { buildNotification(Result.failure(error)) }
			notificationManager.notify(TAG, startId, notification)
		}
	}

	@SuppressLint("InlinedApi")
	private fun startForeground(startId: Int) {
		val title = applicationContext.getString(R.string.fixing_manga)
		val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_MIN)
			.setName(title)
			.setShowBadge(false)
			.setVibrationEnabled(false)
			.setSound(null, null)
			.setLightsEnabled(false)
			.build()
		notificationManager.createNotificationChannel(channel)

		val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
			.setContentTitle(title)
			.setPriority(NotificationCompat.PRIORITY_MIN)
			.setDefaults(0)
			.setSilent(true)
			.setOngoing(true)
			.setProgress(0, 0, true)
			.setSmallIcon(R.drawable.ic_stat_auto_fix)
			.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
			.setCategory(NotificationCompat.CATEGORY_PROGRESS)
			.addAction(
				materialR.drawable.material_ic_clear_black_24dp,
				applicationContext.getString(android.R.string.cancel),
				getCancelIntent(startId),
			)
			.build()

		ServiceCompat.startForeground(
			this,
			FOREGROUND_NOTIFICATION_ID,
			notification,
			ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
		)
	}

	private suspend fun buildNotification(result: Result<Pair<Manga, Manga?>>): Notification {
		val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
			.setPriority(NotificationCompat.PRIORITY_DEFAULT)
			.setDefaults(0)
			.setSilent(true)
			.setAutoCancel(true)
		result.onSuccess { (seed, replacement) ->
			if (replacement != null) {
				notification.setLargeIcon(
					coil.execute(
						ImageRequest.Builder(applicationContext)
							.data(replacement.coverUrl)
							.mangaSourceExtra(replacement.source)
							.build(),
					).toBitmapOrNull(),
				)
				notification.setSubText(replacement.title)
				val intent = DetailsActivity.newIntent(applicationContext, replacement)
				notification.setContentIntent(
					PendingIntentCompat.getActivity(
						applicationContext,
						replacement.id.toInt(),
						intent,
						PendingIntent.FLAG_UPDATE_CURRENT,
						false,
					),
				).setVisibility(
					if (replacement.isNsfw) NotificationCompat.VISIBILITY_SECRET else NotificationCompat.VISIBILITY_PUBLIC,
				)
				notification
					.setContentTitle(applicationContext.getString(R.string.fixed))
					.setContentText(
						applicationContext.getString(
							R.string.manga_replaced,
							seed.title,
							seed.source.getTitle(applicationContext),
							replacement.title,
							replacement.source.getTitle(applicationContext),
						),
					)
					.setSmallIcon(R.drawable.ic_stat_done)
			} else {
				notification
					.setContentTitle(applicationContext.getString(R.string.fixing_manga))
					.setContentText(applicationContext.getString(R.string.no_fix_required, seed.title))
					.setSmallIcon(android.R.drawable.stat_sys_warning)
			}
		}.onFailure { error ->
			notification
				.setContentTitle(applicationContext.getString(R.string.error_occurred))
				.setContentText(
					if (error is AutoFixUseCase.NoAlternativesException) {
						applicationContext.getString(R.string.no_alternatives_found, error.seed.manga.title)
					} else {
						error.getDisplayMessage(applicationContext.resources)
					},
				).setSmallIcon(android.R.drawable.stat_notify_error)
			ErrorReporterReceiver.getPendingIntent(applicationContext, error)?.let { reportIntent ->
				notification.addAction(
					R.drawable.ic_alert_outline,
					applicationContext.getString(R.string.report),
					reportIntent,
				)
			}
		}
		return notification.build()
	}

	companion object {

		private const val DATA_IDS = "ids"
		private const val TAG = "auto_fix"
		private const val CHANNEL_ID = "auto_fix"
		private const val FOREGROUND_NOTIFICATION_ID = 38

		fun start(context: Context, mangaIds: Collection<Long>): Boolean = try {
			val intent = Intent(context, AutoFixService::class.java)
			intent.putExtra(DATA_IDS, mangaIds.toLongArray())
			ContextCompat.startForegroundService(context, intent)
			true
		} catch (e: Exception) {
			e.printStackTraceDebug()
			false
		}
	}
}
