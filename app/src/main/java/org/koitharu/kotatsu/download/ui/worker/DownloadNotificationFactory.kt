package org.koitharu.kotatsu.download.ui.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.format.DateUtils
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.work.WorkManager
import coil.ImageLoader
import coil.request.ImageRequest
import coil.size.Scale
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.details.ui.DetailsActivity
import org.koitharu.kotatsu.download.domain.DownloadState2
import org.koitharu.kotatsu.download.ui.list.DownloadsActivity
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.util.format
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import org.koitharu.kotatsu.search.ui.MangaListActivity
import org.koitharu.kotatsu.utils.ext.getDrawableOrThrow
import org.koitharu.kotatsu.utils.ext.printStackTraceDebug
import java.util.UUID
import com.google.android.material.R as materialR

private const val CHANNEL_ID = "download"
private const val GROUP_ID = "downloads"

class DownloadNotificationFactory @AssistedInject constructor(
	@ApplicationContext private val context: Context,
	private val coil: ImageLoader,
	@Assisted private val uuid: UUID,
) {

	private val covers = HashMap<Manga, Drawable>()
	private val builder = NotificationCompat.Builder(context, CHANNEL_ID)
	private val mutex = Mutex()

	private val coverWidth = context.resources.getDimensionPixelSize(
		androidx.core.R.dimen.compat_notification_large_icon_max_width,
	)
	private val coverHeight = context.resources.getDimensionPixelSize(
		androidx.core.R.dimen.compat_notification_large_icon_max_height,
	)
	private val queueIntent = PendingIntentCompat.getActivity(
		context,
		0,
		DownloadsActivity.newIntent(context),
		0,
		false,
	)

	private val actionCancel by lazy {
		NotificationCompat.Action(
			materialR.drawable.material_ic_clear_black_24dp,
			context.getString(android.R.string.cancel),
			WorkManager.getInstance(context).createCancelPendingIntent(uuid),
		)
	}

	private val actionPause by lazy {
		NotificationCompat.Action(
			R.drawable.ic_action_pause,
			context.getString(R.string.pause),
			PausingReceiver.createPausePendingIntent(context, uuid),
		)
	}

	private val actionResume by lazy {
		NotificationCompat.Action(
			R.drawable.ic_action_resume,
			context.getString(R.string.resume),
			PausingReceiver.createResumePendingIntent(context, uuid),
		)
	}

	init {
		createChannel()
		builder.setOnlyAlertOnce(true)
		builder.setDefaults(0)
		builder.color = ContextCompat.getColor(context, R.color.blue_primary)
		builder.foregroundServiceBehavior = NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
		builder.setSilent(true)
		builder.setGroup(GROUP_ID)
		builder.setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
		builder.priority = NotificationCompat.PRIORITY_DEFAULT
	}

	suspend fun create(state: DownloadState2?): Notification = mutex.withLock {
		builder.setContentTitle(state?.manga?.title ?: context.getString(R.string.preparing_))
		builder.setContentText(context.getString(R.string.manga_downloading_))
		builder.setProgress(1, 0, true)
		builder.setSmallIcon(android.R.drawable.stat_sys_download)
		builder.setContentIntent(queueIntent)
		builder.setStyle(null)
		builder.setLargeIcon(if (state != null) getCover(state.manga)?.toBitmap() else null)
		builder.clearActions()
		builder.setSubText(null)
		builder.setShowWhen(false)
		builder.setVisibility(
			if (state != null && state.manga.isNsfw) {
				NotificationCompat.VISIBILITY_PRIVATE
			} else {
				NotificationCompat.VISIBILITY_PUBLIC
			},
		)
		when {
			state == null -> Unit
			state.localManga != null -> { // downloaded, final state
				builder.setProgress(0, 0, false)
				builder.setContentText(context.getString(R.string.download_complete))
				builder.setContentIntent(createMangaIntent(context, state.localManga.manga))
				builder.setAutoCancel(true)
				builder.setSmallIcon(android.R.drawable.stat_sys_download_done)
				builder.setCategory(null)
				builder.setStyle(null)
				builder.setOngoing(false)
				builder.setShowWhen(true)
				builder.setWhen(System.currentTimeMillis())
			}

			state.isPaused -> { // paused (with error or manually)
				builder.setProgress(state.max, state.progress, false)
				val percent = context.getString(R.string.percent_string_pattern, (state.percent * 100).format())
				builder.setContentText(percent)
				builder.setContentText(state.error)
				builder.setCategory(NotificationCompat.CATEGORY_PROGRESS)
				builder.setStyle(null)
				builder.setOngoing(true)
				builder.setSmallIcon(R.drawable.ic_stat_paused)
				builder.addAction(actionCancel)
				builder.addAction(actionResume)
			}

			state.error != null -> { // error, final state
				builder.setProgress(0, 0, false)
				builder.setSmallIcon(android.R.drawable.stat_notify_error)
				builder.setSubText(context.getString(R.string.error))
				builder.setContentText(state.error)
				builder.setAutoCancel(true)
				builder.setOngoing(false)
				builder.setCategory(NotificationCompat.CATEGORY_ERROR)
				builder.setShowWhen(true)
				builder.setWhen(System.currentTimeMillis())
				builder.setStyle(NotificationCompat.BigTextStyle().bigText(state.error))
			}

			else -> {
				builder.setProgress(state.max, state.progress, false)
				val percent = context.getString(R.string.percent_string_pattern, (state.percent * 100).format())
				if (state.eta > 0L) {
					val eta = DateUtils.getRelativeTimeSpanString(
						state.eta,
						System.currentTimeMillis(),
						DateUtils.SECOND_IN_MILLIS,
					)
					builder.setContentText(eta)
					builder.setSubText(percent)
				} else {
					builder.setContentText(percent)
				}
				builder.setCategory(NotificationCompat.CATEGORY_PROGRESS)
				builder.setStyle(null)
				builder.setOngoing(true)
				builder.addAction(actionCancel)
				builder.addAction(actionPause)
			}
		}
		return builder.build()
	}

	private fun createMangaIntent(context: Context, manga: Manga?) = PendingIntentCompat.getActivity(
		context,
		manga.hashCode(),
		if (manga != null) {
			DetailsActivity.newIntent(context, manga)
		} else {
			MangaListActivity.newIntent(context, MangaSource.LOCAL)
		},
		PendingIntent.FLAG_CANCEL_CURRENT,
		false,
	)

	private suspend fun getCover(manga: Manga) = covers[manga] ?: run {
		runCatchingCancellable {
			coil.execute(
				ImageRequest.Builder(context)
					.data(manga.coverUrl)
					.allowHardware(false)
					.tag(manga.source)
					.size(coverWidth, coverHeight)
					.scale(Scale.FILL)
					.build(),
			).getDrawableOrThrow()
		}.onSuccess {
			covers[manga] = it
		}.onFailure {
			it.printStackTraceDebug()
		}.getOrNull()
	}

	private fun createChannel() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val manager = NotificationManagerCompat.from(context)
			if (manager.getNotificationChannel(CHANNEL_ID) == null) {
				val channel = NotificationChannel(
					CHANNEL_ID,
					context.getString(R.string.downloads),
					NotificationManager.IMPORTANCE_LOW,
				)
				channel.enableVibration(false)
				channel.enableLights(false)
				channel.setSound(null, null)
				manager.createNotificationChannel(channel)
			}
		}
	}

	@AssistedFactory
	interface Factory {

		fun create(uuid: UUID): DownloadNotificationFactory
	}
}
