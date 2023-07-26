package org.koitharu.kotatsu.tracker.work

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationChannelGroupCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.core.prefs.AppSettings

class TrackerNotificationChannels @Inject constructor(
	@ApplicationContext private val context: Context,
	private val settings: AppSettings,
) {

	private val manager = NotificationManagerCompat.from(context)

	val areNotificationsDisabled: Boolean
		get() = !manager.areNotificationsEnabled()

	fun updateChannels(categories: Collection<FavouriteCategory>) {
		manager.deleteNotificationChannel(OLD_CHANNEL_ID)
		val group = createGroup()
		val existingChannels = group.channels.associateByTo(HashMap()) { it.id }
		for (category in categories) {
			val id = getFavouritesChannelId(category.id)
			if (existingChannels.remove(id)?.name == category.title) {
				continue
			}
			val channel = NotificationChannelCompat.Builder(id, NotificationManagerCompat.IMPORTANCE_DEFAULT)
				.setName(category.title)
				.setGroup(GROUP_ID)
				.build()
			manager.createNotificationChannel(channel)
		}
		existingChannels.remove(CHANNEL_ID_HISTORY)
		createHistoryChannel()
		for (id in existingChannels.keys) {
			manager.deleteNotificationChannel(id)
		}
	}

	fun createChannel(category: FavouriteCategory) {
		val id = getFavouritesChannelId(category.id)
		val channel = NotificationChannelCompat.Builder(id, NotificationManagerCompat.IMPORTANCE_DEFAULT)
			.setName(category.title)
			.setGroup(createGroup().id)
			.build()
		manager.createNotificationChannel(channel)
	}

	fun deleteChannel(categoryId: Long) {
		manager.deleteNotificationChannel(getFavouritesChannelId(categoryId))
	}

	fun isFavouriteNotificationsEnabled(category: FavouriteCategory): Boolean {
		if (!manager.areNotificationsEnabled()) {
			return false
		}
		return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val channel = manager.getNotificationChannel(getFavouritesChannelId(category.id))
			channel != null && channel.importance != NotificationManager.IMPORTANCE_NONE
		} else {
			// fallback
			settings.isTrackerNotificationsEnabled
		}
	}

	fun isHistoryNotificationsEnabled(): Boolean {
		if (!manager.areNotificationsEnabled()) {
			return false
		}
		return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val channel = manager.getNotificationChannel(getHistoryChannelId())
			channel != null && channel.importance != NotificationManager.IMPORTANCE_NONE
		} else {
			// fallback
			settings.isTrackerNotificationsEnabled
		}
	}

	fun isNotificationGroupEnabled(): Boolean {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
			return settings.isTrackerNotificationsEnabled
		}
		val group = manager.getNotificationChannelGroupCompat(GROUP_ID) ?: return true
		return !group.isBlocked && group.channels.any { it.importance != NotificationManagerCompat.IMPORTANCE_NONE }
	}

	fun getFavouritesChannelId(categoryId: Long): String {
		return CHANNEL_ID_PREFIX + categoryId
	}

	fun getHistoryChannelId(): String {
		return CHANNEL_ID_HISTORY
	}

	private fun createGroup(): NotificationChannelGroupCompat {
		return manager.getNotificationChannelGroupCompat(GROUP_ID) ?: run {
			val group = NotificationChannelGroupCompat.Builder(GROUP_ID)
				.setName(context.getString(R.string.new_chapters))
				.build()
			manager.createNotificationChannelGroup(group)
			group
		}
	}

	private fun createHistoryChannel() {
		val channel = NotificationChannelCompat.Builder(CHANNEL_ID_HISTORY, NotificationManagerCompat.IMPORTANCE_DEFAULT)
			.setName(context.getString(R.string.history))
			.setGroup(GROUP_ID)
			.build()
		manager.createNotificationChannel(channel)
	}

	companion object {

		const val GROUP_ID = "trackers"
		private const val CHANNEL_ID_PREFIX = "track_fav_"
		private const val CHANNEL_ID_HISTORY = "track_history"
		private const val OLD_CHANNEL_ID = "tracking"
	}
}
