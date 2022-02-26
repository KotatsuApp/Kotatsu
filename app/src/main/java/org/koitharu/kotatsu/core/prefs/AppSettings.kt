package org.koitharu.kotatsu.core.prefs

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.appcompat.app.AppCompatDelegate
import androidx.collection.arraySetOf
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.google.android.material.color.DynamicColors
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.callbackFlow
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.core.model.ZoomMode
import org.koitharu.kotatsu.utils.ext.toUriOrNull
import java.io.File
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class AppSettings(context: Context) {

	private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

	var listMode: ListMode
		get() = prefs.getString(KEY_LIST_MODE, null)?.findEnumValue(ListMode.values()) ?: ListMode.DETAILED_LIST
		set(value) = prefs.edit { putString(KEY_LIST_MODE, value.name) }

	var defaultSection: AppSection
		get() = prefs.getString(KEY_APP_SECTION, null)?.findEnumValue(AppSection.values()) ?: AppSection.HISTORY
		set(value) = prefs.edit { putString(KEY_APP_SECTION, value.name) }

	val theme: Int
		get() = prefs.getString(KEY_THEME, null)?.toIntOrNull() ?: AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM

	val isDynamicTheme: Boolean
		get() = prefs.getBoolean(KEY_DYNAMIC_THEME, false)

	val isAmoledTheme: Boolean
		get() = prefs.getBoolean(KEY_THEME_AMOLED, false)

	val isToolbarHideWhenScrolling: Boolean
		get() = prefs.getBoolean(KEY_HIDE_TOOLBAR, true)

	var gridSize: Int
		get() = prefs.getInt(KEY_GRID_SIZE, 100)
		set(value) = prefs.edit { putInt(KEY_GRID_SIZE, value) }

	val readerPageSwitch: Set<String>
		get() = prefs.getStringSet(KEY_READER_SWITCHERS, null) ?: setOf(PAGE_SWITCH_TAPS)

	var isTrafficWarningEnabled: Boolean
		get() = prefs.getBoolean(KEY_TRAFFIC_WARNING, true)
		set(value) = prefs.edit { putBoolean(KEY_TRAFFIC_WARNING, value) }

	val appUpdateAuto: Boolean
		get() = prefs.getBoolean(KEY_APP_UPDATE_AUTO, true)

	var appUpdate: Long
		get() = prefs.getLong(KEY_APP_UPDATE, 0L)
		set(value) = prefs.edit { putLong(KEY_APP_UPDATE, value) }

	val trackerNotifications: Boolean
		get() = prefs.getBoolean(KEY_TRACKER_NOTIFICATIONS, true)

	var notificationSound: Uri
		get() = prefs.getString(KEY_NOTIFICATIONS_SOUND, null)?.toUriOrNull()
			?: Settings.System.DEFAULT_NOTIFICATION_URI
		set(value) = prefs.edit { putString(KEY_NOTIFICATIONS_SOUND, value.toString()) }

	val notificationVibrate: Boolean
		get() = prefs.getBoolean(KEY_NOTIFICATIONS_VIBRATE, false)

	val notificationLight: Boolean
		get() = prefs.getBoolean(KEY_NOTIFICATIONS_LIGHT, true)

	val readerAnimation: Boolean
		get() = prefs.getBoolean(KEY_READER_ANIMATION, false)

	val isPreferRtlReader: Boolean
		get() = prefs.getBoolean(KEY_READER_PREFER_RTL, false)

	var historyGrouping: Boolean
		get() = prefs.getBoolean(KEY_HISTORY_GROUPING, true)
		set(value) = prefs.edit { putBoolean(KEY_HISTORY_GROUPING, value) }

	val isHistoryExcludeNsfw: Boolean
		get() = prefs.getBoolean(KEY_HISTORY_EXCLUDE_NSFW, false)

	var chaptersReverse: Boolean
		get() = prefs.getBoolean(KEY_REVERSE_CHAPTERS, false)
		set(value) = prefs.edit { putBoolean(KEY_REVERSE_CHAPTERS, value) }

	val zoomMode: ZoomMode
		get() = prefs.getString(KEY_ZOOM_MODE, null)?.findEnumValue(ZoomMode.values()) ?: ZoomMode.FIT_CENTER

	val trackSources: Set<String>
		get() = prefs.getStringSet(KEY_TRACK_SOURCES, null) ?: arraySetOf(TRACK_FAVOURITES, TRACK_HISTORY)

	var appPassword: String?
		get() = prefs.getString(KEY_APP_PASSWORD, null)
		set(value) = prefs.edit { if (value != null) putString(KEY_APP_PASSWORD, value) else remove(KEY_APP_PASSWORD) }

	var sourcesOrder: List<Int>
		get() = prefs.getString(KEY_SOURCES_ORDER, null)
			?.split('|')
			?.mapNotNull(String::toIntOrNull)
			.orEmpty()
		set(value) = prefs.edit {
			putString(KEY_SOURCES_ORDER, value.joinToString("|"))
		}

	var hiddenSources: Set<String>
		get() = prefs.getStringSet(KEY_SOURCES_HIDDEN, null) ?: emptySet()
		set(value) = prefs.edit { putStringSet(KEY_SOURCES_HIDDEN, value) }

	val isSourcesSelected: Boolean
		get() = KEY_SOURCES_HIDDEN in prefs

	val isPagesNumbersEnabled: Boolean
		get() = prefs.getBoolean(KEY_PAGES_NUMBERS, false)

	var mangaStorageDir: File?
		get() = prefs.getString(KEY_LOCAL_STORAGE, null)?.let {
			File(it)
		}?.takeIf { it.exists() }
		set(value) = prefs.edit {
			if (value == null) {
				remove(KEY_LOCAL_STORAGE)
			} else {
				putString(KEY_LOCAL_STORAGE, value.path)
			}
		}

	fun getDateFormat(format: String = prefs.getString(KEY_DATE_FORMAT, "").orEmpty()): DateFormat =
		when (format) {
			"" -> DateFormat.getDateInstance(DateFormat.SHORT)
			else -> SimpleDateFormat(format, Locale.getDefault())
		}

	fun getMangaSources(includeHidden: Boolean): List<MangaSource> {
		val list = MangaSource.values().toMutableList()
		list.remove(MangaSource.LOCAL)
		val order = sourcesOrder
		list.sortBy { x ->
			val e = order.indexOf(x.ordinal)
			if (e == -1) order.size + x.ordinal else e
		}
		if (!includeHidden) {
			val hidden = hiddenSources
			list.removeAll { x -> x.name in hidden }
		}
		return list
	}

	fun subscribe(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
		prefs.registerOnSharedPreferenceChangeListener(listener)
	}

	fun unsubscribe(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
		prefs.unregisterOnSharedPreferenceChangeListener(listener)
	}

	fun observe() = callbackFlow<String> {
		val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
			trySendBlocking(key)
		}
		prefs.registerOnSharedPreferenceChangeListener(listener)
		awaitClose {
			prefs.unregisterOnSharedPreferenceChangeListener(listener)
		}
	}

	private fun <E : Enum<E>> String.findEnumValue(values: Array<E>): E? {
		return values.find { it.name == this }
	}

	companion object {

		const val PAGE_SWITCH_TAPS = "taps"
		const val PAGE_SWITCH_VOLUME_KEYS = "volume"

		const val TRACK_HISTORY = "history"
		const val TRACK_FAVOURITES = "favourites"

		const val KEY_LIST_MODE = "list_mode_2"
		const val KEY_APP_SECTION = "app_section_2"
		const val KEY_THEME = "theme"
		const val KEY_DYNAMIC_THEME = "dynamic_theme"
		const val KEY_THEME_AMOLED = "amoled_theme"
		const val KEY_DATE_FORMAT = "date_format"
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
		const val KEY_HISTORY_EXCLUDE_NSFW = "history_exclude_nsfw"
		const val KEY_PAGES_NUMBERS = "pages_numbers"

		// About
		const val KEY_APP_UPDATE = "app_update"
		const val KEY_APP_UPDATE_AUTO = "app_update_auto"
		const val KEY_APP_TRANSLATION = "about_app_translation"
		const val KEY_APP_GRATITUDES = "about_gratitudes"
		const val KEY_FEEDBACK_4PDA = "about_feedback_4pda"
		const val KEY_FEEDBACK_DISCORD = "about_feedback_discord"
		const val KEY_FEEDBACK_GITHUB = "about_feedback_github"
		const val KEY_SUPPORT_DEVELOPER = "about_support_developer"

		val isDynamicColorAvailable: Boolean
			get() = DynamicColors.isDynamicColorAvailable() ||
				(isSamsung && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)

		private val isSamsung
			get() = Build.MANUFACTURER.equals("samsung", ignoreCase = true)
	}
}
