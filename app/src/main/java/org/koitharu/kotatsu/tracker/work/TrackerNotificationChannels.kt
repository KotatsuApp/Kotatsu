package org.koitharu.kotatsu.tracker.work

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
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
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
			return
		}
		manager.deleteNotificationChannel(OLD_CHANNEL_ID)
		val group = createGroup()
		val existingChannels = group.channels.associateByTo(HashMap()) { it.id }
		for (category in categories) {
			val id = getFavouritesChannelId(category.id)
			if (existingChannels.remove(id)?.name == category.title) {
				continue
			}
			val channel = NotificationChannel(id, category.title, NotificationManager.IMPORTANCE_DEFAULT)
			channel.group = GROUP_ID
			manager.createNotificationChannel(channel)
		}
		existingChannels.remove(CHANNEL_ID_HISTORY)
		createHistoryChannel()
		for (id in existingChannels.keys) {
			manager.deleteNotificationChannel(id)
		}
	}

	fun createChannel(category: FavouriteCategory) {
		renameChannel(category.id, category.title)
	}

	fun renameChannel(categoryId: Long, name: String) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
			return
		}
		val id = getFavouritesChannelId(categoryId)
		val channel = NotificationChannel(id, name, NotificationManager.IMPORTANCE_DEFAULT)
		channel.group = createGroup().id
		manager.createNotificationChannel(channel)
	}

	fun deleteChannel(categoryId: Long) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
			return
		}
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
		val group = manager.getNotificationChannelGroup(GROUP_ID) ?: return true
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && group.isBlocked) {
			return false
		}
		return group.channels.any { it.importance != NotificationManagerCompat.IMPORTANCE_NONE }
	}

	fun getFavouritesChannelId(categoryId: Long): String {
		return CHANNEL_ID_PREFIX + categoryId
	}

	fun getHistoryChannelId(): String {
		return CHANNEL_ID_HISTORY
	}

	@RequiresApi(Build.VERSION_CODES.O)
	private fun createGroup(): NotificationChannelGroup {
		manager.getNotificationChannelGroup(GROUP_ID)?.let {
			return it
		}
		val group = NotificationChannelGroup(GROUP_ID, context.getString(R.string.new_chapters))
		manager.createNotificationChannelGroup(group)
		return group
	}

	private fun createHistoryChannel() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
			return
		}
		val channel = NotificationChannel(
			CHANNEL_ID_HISTORY,
			context.getString(R.string.history),
			NotificationManager.IMPORTANCE_DEFAULT,
		)
		channel.group = GROUP_ID
		manager.createNotificationChannel(channel)
	}

	companion object {

		const val GROUP_ID = "trackers"
		private const val CHANNEL_ID_PREFIX = "track_fav_"
		private const val CHANNEL_ID_HISTORY = "track_history"
		private const val OLD_CHANNEL_ID = "tracking"
	}
}
