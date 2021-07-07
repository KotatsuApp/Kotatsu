package org.koitharu.kotatsu.core.prefs

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import androidx.appcompat.app.AppCompatDelegate
import androidx.collection.arraySetOf
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.callbackFlow
import org.koitharu.kotatsu.core.model.ZoomMode
import org.koitharu.kotatsu.local.domain.LocalMangaRepository
import org.koitharu.kotatsu.utils.delegates.prefs.*
import java.io.File

class AppSettings private constructor(private val prefs: SharedPreferences) :
	SharedPreferences by prefs {

	constructor(context: Context) : this(
		PreferenceManager.getDefaultSharedPreferences(context)
	)

	var listMode by EnumPreferenceDelegate(
		ListMode::class.java,
		KEY_LIST_MODE,
		ListMode.DETAILED_LIST
	)

	var defaultSection by IntEnumPreferenceDelegate(
		AppSection::class.java,
		KEY_APP_SECTION,
		AppSection.HISTORY
	)

	val theme by StringIntPreferenceDelegate(
		KEY_THEME,
		AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
	)

	val isAmoledTheme by BoolPreferenceDelegate(KEY_THEME_AMOLED, defaultValue = false)

	val isToolbarHideWhenScrolling by BoolPreferenceDelegate(KEY_HIDE_TOOLBAR, defaultValue = true)

	var gridSize by IntPreferenceDelegate(KEY_GRID_SIZE, defaultValue = 100)

	val readerPageSwitch by StringSetPreferenceDelegate(
		KEY_READER_SWITCHERS,
		arraySetOf(PAGE_SWITCH_TAPS)
	)

	var isTrafficWarningEnabled by BoolPreferenceDelegate(KEY_TRAFFIC_WARNING, defaultValue = true)

	val appUpdateAuto by BoolPreferenceDelegate(KEY_APP_UPDATE_AUTO, defaultValue = true)

	var appUpdate by LongPreferenceDelegate(KEY_APP_UPDATE, defaultValue = 0L)

	val trackerNotifications by BoolPreferenceDelegate(
		KEY_TRACKER_NOTIFICATIONS,
		defaultValue = true
	)

	var notificationSound by StringPreferenceDelegate(
		KEY_NOTIFICATIONS_SOUND,
		Settings.System.DEFAULT_NOTIFICATION_URI.toString()
	)

	val notificationVibrate by BoolPreferenceDelegate(KEY_NOTIFICATIONS_VIBRATE, false)

	val notificationLight by BoolPreferenceDelegate(KEY_NOTIFICATIONS_LIGHT, true)

	val readerAnimation by BoolPreferenceDelegate(KEY_READER_ANIMATION, false)

	val isPreferRtlReader by BoolPreferenceDelegate(KEY_READER_PREFER_RTL, false)

	var historyGrouping by BoolPreferenceDelegate(KEY_HISTORY_GROUPING, true)

	var chaptersReverse by BoolPreferenceDelegate(KEY_REVERSE_CHAPTERS, false)

	val zoomMode by EnumPreferenceDelegate(
		ZoomMode::class.java,
		KEY_ZOOM_MODE,
		ZoomMode.FIT_CENTER
	)

	val trackSources by StringSetPreferenceDelegate(
		KEY_TRACK_SOURCES,
		arraySetOf(TRACK_FAVOURITES, TRACK_HISTORY)
	)

	var appPassword by NullableStringPreferenceDelegate(KEY_APP_PASSWORD)

	private var sourcesOrderStr by NullableStringPreferenceDelegate(KEY_SOURCES_ORDER)

	var sourcesOrder: List<Int>
		get() = sourcesOrderStr?.split('|')?.mapNotNull(String::toIntOrNull).orEmpty()
		set(value) {
			sourcesOrderStr = value.joinToString("|")
		}

	var hiddenSources by StringSetPreferenceDelegate(KEY_SOURCES_HIDDEN)

	val isSourcesSelected: Boolean
		get() = KEY_SOURCES_HIDDEN in prefs

	fun getStorageDir(context: Context): File? {
		val value = prefs.getString(KEY_LOCAL_STORAGE, null)?.let {
			File(it)
		}?.takeIf { it.exists() && it.canWrite() }
		return value ?: LocalMangaRepository.getFallbackStorageDir(context)
	}

	fun setStorageDir(context: Context, file: File?) {
		prefs.edit {
			if (file == null) {
				remove(KEY_LOCAL_STORAGE)
			} else {
				putString(KEY_LOCAL_STORAGE, file.path)
			}
		}
	}

	@Deprecated("Use observe()")
	fun subscribe(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
		prefs.registerOnSharedPreferenceChangeListener(listener)
	}

	fun unsubscribe(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
		prefs.unregisterOnSharedPreferenceChangeListener(listener)
	}

	fun observe() = callbackFlow<String> {
		val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
			sendBlocking(key)
		}
		prefs.registerOnSharedPreferenceChangeListener(listener)
		awaitClose {
			prefs.unregisterOnSharedPreferenceChangeListener(listener)
		}
	}

	companion object {

		const val PAGE_SWITCH_TAPS = "taps"
		const val PAGE_SWITCH_VOLUME_KEYS = "volume"

		const val TRACK_HISTORY = "history"
		const val TRACK_FAVOURITES = "favourites"

		const val KEY_LIST_MODE = "list_mode_2"
		const val KEY_APP_SECTION = "app_section"
		const val KEY_THEME = "theme"
		const val KEY_THEME_AMOLED = "amoled_theme"
		const val KEY_HIDE_TOOLBAR = "hide_toolbar"
		const val KEY_SOURCES_ORDER = "sources_order"
		const val KEY_SOURCES_HIDDEN = "sources_hidden"
		const val KEY_TRAFFIC_WARNING = "traffic_warning"
		const val KEY_PAGES_CACHE_CLEAR = "pages_cache_clear"
		const val KEY_COOKIES_CLEAR = "cookies_clear"
		const val KEY_THUMBS_CACHE_CLEAR = "thumbs_cache_clear"
		const val KEY_SEARCH_HISTORY_CLEAR = "search_history_clear"
		const val KEY_UPDATES_FEED_CLEAR = "updates_feed_clear"
		const val KEY_GRID_SIZE = "grid_size"
		const val KEY_REMOTE_SOURCES = "remote_sources"
		const val KEY_LOCAL_STORAGE = "local_storage"
		const val KEY_READER_SWITCHERS = "reader_switchers"
		const val KEY_TRACK_SOURCES = "track_sources"
		const val KEY_TRACK_WARNING = "track_warning"
		const val KEY_APP_UPDATE = "app_update"
		const val KEY_APP_UPDATE_AUTO = "app_update_auto"
		const val KEY_TRACKER_NOTIFICATIONS = "tracker_notifications"
		const val KEY_NOTIFICATIONS_SETTINGS = "notifications_settings"
		const val KEY_NOTIFICATIONS_SOUND = "notifications_sound"
		const val KEY_NOTIFICATIONS_VIBRATE = "notifications_vibrate"
		const val KEY_NOTIFICATIONS_LIGHT = "notifications_light"
		const val KEY_READER_ANIMATION = "reader_animation"
		const val KEY_READER_PREFER_RTL = "reader_prefer_rtl"
		const val KEY_APP_PASSWORD = "app_password"
		const val KEY_PROTECT_APP = "protect_app"
		const val KEY_APP_VERSION = "app_version"
		const val KEY_ZOOM_MODE = "zoom_mode"
		const val KEY_BACKUP = "backup"
		const val KEY_RESTORE = "restore"
		const val KEY_HISTORY_GROUPING = "history_grouping"
		const val KEY_REVERSE_CHAPTERS = "reverse_chapters"
	}
}