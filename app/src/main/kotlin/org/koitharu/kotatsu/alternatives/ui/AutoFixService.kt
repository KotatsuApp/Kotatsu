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
import androidx.core.content.ContextCompat
import coil3.ImageLoader
import coil3.request.ImageRequest
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.alternatives.domain.AutoFixUseCase
import org.koitharu.kotatsu.alternatives.domain.AutoFixUseCase.NoAlternativesException
import org.koitharu.kotatsu.core.ErrorReporterReceiver
import org.koitharu.kotatsu.core.model.getTitle
import org.koitharu.kotatsu.core.model.isNsfw
import org.koitharu.kotatsu.core.nav.AppRouter
import org.koitharu.kotatsu.core.ui.CoroutineIntentService
import org.koitharu.kotatsu.core.util.ext.checkNotificationPermission
import org.koitharu.kotatsu.core.util.ext.getDisplayMessage
import org.koitharu.kotatsu.core.util.ext.mangaSourceExtra
import org.koitharu.kotatsu.core.util.ext.powerManager
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import org.koitharu.kotatsu.core.util.ext.toBitmapOrNull
import org.koitharu.kotatsu.core.util.ext.withPartialWakeLock
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import javax.inject.Inject
import androidx.appcompat.R as appcompatR

@AndroidEntryPoint
class AutoFixService : CoroutineIntentService() {

	@Inject
	lateinit var autoFixUseCase: AutoFixUseCase

	@Inject
	lateinit var coil: ImageLoader

	private lateinit var notificationManager: NotificationManagerCompat

	override fun onCreate() {
		super.onCreate()
		notificationManager = NotificationManagerCompat.from(this)
	}

	override suspend fun IntentJobContext.processIntent(intent: Intent) {
		val ids = requireNotNull(intent.getLongArrayExtra(DATA_IDS))
		startForeground(this)
		for (mangaId in ids) {
			powerManager.withPartialWakeLock(TAG) {
				val result = runCatchingCancellable {
					autoFixUseCase.invoke(mangaId)
				}
				if (checkNotificationPermission(CHANNEL_ID)) {
					val notification = buildNotification(startId, result)
					notificationManager.notify(TAG, startId, notification)
				}
			}
		}
	}

	override fun IntentJobContext.onError(error: Throwable) {
		if (checkNotificationPermission(CHANNEL_ID)) {
			val notification = runBlocking { buildNotification(startId, Result.failure(error)) }
			notificationManager.notify(TAG, startId, notification)
		}
	}

	@SuppressLint("InlinedApi")
	private fun startForeground(jobContext: IntentJobContext) {
		val title = getString(R.string.fixing_manga)
		val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_MIN)
			.setName(title)
			.setShowBadge(false)
			.setVibrationEnabled(false)
			.setSound(null, null)
			.setLightsEnabled(false)
			.build()
		notificationManager.createNotificationChannel(channel)

		val notification = NotificationCompat.Builder(this, CHANNEL_ID)
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
				appcompatR.drawable.abc_ic_clear_material,
				getString(android.R.string.cancel),
				jobContext.getCancelIntent(),
			)
			.build()

		jobContext.setForeground(
			FOREGROUND_NOTIFICATION_ID,
			notification,
			ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
		)
	}

	private suspend fun buildNotification(startId: Int, result: Result<Pair<Manga, Manga?>>): Notification {
		val notification = NotificationCompat.Builder(this, CHANNEL_ID)
			.setPriority(NotificationCompat.PRIORITY_DEFAULT)
			.setDefaults(0)
			.setSilent(true)
			.setAutoCancel(true)
		result.onSuccess { (seed, replacement) ->
			if (replacement != null) {
				notification.setLargeIcon(
					coil.execute(
						ImageRequest.Builder(this)
							.data(replacement.coverUrl)
							.mangaSourceExtra(replacement.source)
							.build(),
					).toBitmapOrNull(),
				)
				notification.setSubText(replacement.title)
				val intent = AppRouter.detailsIntent(this, replacement)
				notification.setContentIntent(
					PendingIntentCompat.getActivity(
						this,
						replacement.id.toInt(),
						intent,
						PendingIntent.FLAG_UPDATE_CURRENT,
						false,
					),
				).setVisibility(
					if (replacement.isNsfw()) {
						NotificationCompat.VISIBILITY_SECRET
					} else {
						NotificationCompat.VISIBILITY_PUBLIC
					},
				)
				notification
					.setContentTitle(getString(R.string.fixed))
					.setContentText(
						getString(
							R.string.manga_replaced,
							seed.title,
							seed.source.getTitle(this),
							replacement.title,
							replacement.source.getTitle(this),
						),
					)
					.setSmallIcon(R.drawable.ic_stat_done)
			} else {
				notification
					.setContentTitle(getString(R.string.fixing_manga))
					.setContentText(getString(R.string.no_fix_required, seed.title))
					.setSmallIcon(android.R.drawable.stat_sys_warning)
			}
		}.onFailure { error ->
			notification
				.setContentTitle(getString(R.string.error_occurred))
				.setContentText(
					if (error is NoAlternativesException) {
						getString(R.string.no_alternatives_found, error.seed.manga.title)
					} else {
						error.getDisplayMessage(resources)
					},
				).setSmallIcon(android.R.drawable.stat_notify_error)
			ErrorReporterReceiver.getNotificationAction(
				context = this,
				e = error,
				notificationId = startId,
				notificationTag = TAG,
			)?.let { action ->
				notification.addAction(action)
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
