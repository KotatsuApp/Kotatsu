package org.koitharu.kotatsu.tracker.work

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC
import androidx.core.app.NotificationCompat.VISIBILITY_SECRET
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.content.ContextCompat
import coil3.ImageLoader
import coil3.request.ImageRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.util.ext.checkNotificationPermission
import org.koitharu.kotatsu.core.util.ext.mangaSourceExtra
import org.koitharu.kotatsu.core.util.ext.toBitmapOrNull
import org.koitharu.kotatsu.details.ui.DetailsActivity
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.tracker.ui.updates.UpdatesActivity
import javax.inject.Inject

class TrackerNotificationHelper @Inject constructor(
	@ApplicationContext private val applicationContext: Context,
	private val settings: AppSettings,
	private val coil: ImageLoader,
) {

	fun getAreNotificationsEnabled(): Boolean {
		val manager = NotificationManagerCompat.from(applicationContext)
		if (!manager.areNotificationsEnabled()) {
			return false
		}
		return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val channel = manager.getNotificationChannel(CHANNEL_ID)
			channel != null && channel.importance != NotificationManager.IMPORTANCE_NONE
		} else {
			// fallback
			settings.isTrackerNotificationsEnabled
		}
	}

	suspend fun createNotification(manga: Manga, newChapters: List<MangaChapter>): NotificationInfo? {
		if (newChapters.isEmpty() || !applicationContext.checkNotificationPermission(CHANNEL_ID)) {
			return null
		}
		if (manga.isNsfw && (settings.isTrackerNsfwDisabled || settings.isNsfwContentDisabled)) {
			return null
		}
		val id = manga.url.hashCode()
		val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
		val summary = applicationContext.resources.getQuantityString(
			R.plurals.new_chapters,
			newChapters.size,
			newChapters.size,
		)
		with(builder) {
			setContentText(summary)
			setContentTitle(manga.title)
			setNumber(newChapters.size)
			setLargeIcon(
				coil.execute(
					ImageRequest.Builder(applicationContext)
						.data(manga.coverUrl)
						.mangaSourceExtra(manga.source)
						.build(),
				).toBitmapOrNull(),
			)
			setSmallIcon(R.drawable.ic_stat_book_plus)
			setGroup(GROUP_NEW_CHAPTERS)
			val style = NotificationCompat.InboxStyle(this)
			for (chapter in newChapters) {
				style.addLine(chapter.name)
			}
			style.setSummaryText(manga.title)
			style.setBigContentTitle(summary)
			setStyle(style)
			val intent = DetailsActivity.newIntent(applicationContext, manga)
			setContentIntent(
				PendingIntentCompat.getActivity(
					applicationContext,
					id,
					intent,
					PendingIntent.FLAG_UPDATE_CURRENT,
					false,
				),
			)
			setVisibility(if (manga.isNsfw) VISIBILITY_SECRET else VISIBILITY_PUBLIC)
			setShortcutId(manga.id.toString())
			applyCommonSettings(this)
		}
		return NotificationInfo(id, TAG, builder.build(), manga, newChapters.size)
	}

	fun createGroupNotification(
		notifications: List<NotificationInfo>
	): Notification? {
		if (notifications.size <= 1) {
			return null
		}
		val newChaptersCount = notifications.sumOf { it.newChapters }
		val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
		with(builder) {
			val title = applicationContext.resources.getQuantityString(
				R.plurals.new_chapters,
				newChaptersCount,
				newChaptersCount,
			)
			setContentTitle(title)
			setContentText(notifications.joinToString { it.manga.title })
			setSmallIcon(R.drawable.ic_stat_book_plus)
			val style = NotificationCompat.InboxStyle(this)
			for (item in notifications) {
				style.addLine(
					applicationContext.getString(R.string.new_chapters_pattern, item.manga.title, item.newChapters),
				)
			}
			style.setBigContentTitle(title)
			setStyle(style)
			setNumber(newChaptersCount)
			setGroup(GROUP_NEW_CHAPTERS)
			setGroupSummary(true)
			val intent = UpdatesActivity.newIntent(applicationContext)
			setContentIntent(
				PendingIntentCompat.getActivity(
					applicationContext,
					GROUP_NOTIFICATION_ID,
					intent,
					PendingIntent.FLAG_UPDATE_CURRENT,
					false,
				),
			)
			applyCommonSettings(this)
		}
		return builder.build()
	}

	fun updateChannels() {
		val manager = NotificationManagerCompat.from(applicationContext)
		manager.deleteNotificationChannel(LEGACY_CHANNEL_ID)
		manager.deleteNotificationChannel(LEGACY_CHANNEL_ID_HISTORY)
		manager.deleteNotificationChannelGroup(LEGACY_CHANNELS_GROUP_ID)

		val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
			.setName(applicationContext.getString(R.string.new_chapters))
			.setDescription(applicationContext.getString(R.string.show_notification_new_chapters_on))
			.setShowBadge(true)
			.setLightColor(ContextCompat.getColor(applicationContext, R.color.blue_primary))
			.build()
		manager.createNotificationChannel(channel)
	}

	private fun applyCommonSettings(builder: NotificationCompat.Builder) {
		builder.setAutoCancel(true)
		builder.setCategory(NotificationCompat.CATEGORY_SOCIAL)
		builder.priority = NotificationCompat.PRIORITY_DEFAULT
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
			builder.setSound(settings.notificationSound)
			var defaults = if (settings.notificationLight) {
				builder.setLights(ContextCompat.getColor(applicationContext, R.color.blue_primary), 1000, 5000)
				NotificationCompat.DEFAULT_LIGHTS
			} else 0
			if (settings.notificationVibrate) {
				builder.setVibrate(longArrayOf(500, 500, 500, 500))
				defaults = defaults or NotificationCompat.DEFAULT_VIBRATE
			}
			builder.setDefaults(defaults)
		}
	}

	class NotificationInfo(
		val id: Int,
		val tag: String,
		val notification: Notification,
		val manga: Manga,
		val newChapters: Int,
	)

	companion object {

		const val CHANNEL_ID = "tracker_chapters"
		const val GROUP_NOTIFICATION_ID = 0
		const val GROUP_NEW_CHAPTERS = "org.koitharu.kotatsu.NEW_CHAPTERS"
		const val TAG = "tracker"

		private const val LEGACY_CHANNELS_GROUP_ID = "trackers"
		private const val LEGACY_CHANNEL_ID_PREFIX = "track_fav_"
		private const val LEGACY_CHANNEL_ID_HISTORY = "track_history"
		private const val LEGACY_CHANNEL_ID = "tracking"
	}
}
