package org.koitharu.kotatsu.core.prefs

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Uri
import android.provider.Settings
import androidx.appcompat.app.AppCompatDelegate
import androidx.collection.arraySetOf
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.google.android.material.color.DynamicColors
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.core.model.ZoomMode
import org.koitharu.kotatsu.core.network.DoHProvider
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.utils.ext.getEnumValue
import org.koitharu.kotatsu.utils.ext.observe
import org.koitharu.kotatsu.utils.ext.putEnumValue
import org.koitharu.kotatsu.utils.ext.toUriOrNull

@Singleton
class AppSettings @Inject constructor(@ApplicationContext context: Context) {

	private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

	private val remoteSources = EnumSet.allOf(MangaSource::class.java).apply {
		remove(MangaSource.LOCAL)
		if (!BuildConfig.DEBUG) {
			remove(MangaSource.DUMMY)
		}
	}

	val remoteMangaSources: Set<MangaSource>
		get() = Collections.unmodifiableSet(remoteSources)

	var listMode: ListMode
		get() = prefs.getEnumValue(KEY_LIST_MODE, ListMode.GRID)
		set(value) = prefs.edit { putEnumValue(KEY_LIST_MODE, value) }

	var defaultSection: AppSection
		get() = prefs.getEnumValue(KEY_APP_SECTION, AppSection.HISTORY)
		set(value) = prefs.edit { putEnumValue(KEY_APP_SECTION, value) }

	val theme: Int
		get() = prefs.getString(KEY_THEME, null)?.toIntOrNull() ?: AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM

	val isDynamicTheme: Boolean
		get() = DynamicColors.isDynamicColorAvailable() && prefs.getBoolean(KEY_DYNAMIC_THEME, false)

	val isAmoledTheme: Boolean
		get() = prefs.getBoolean(KEY_THEME_AMOLED, false)

	var gridSize: Int
		get() = prefs.getInt(KEY_GRID_SIZE, 100)
		set(value) = prefs.edit { putInt(KEY_GRID_SIZE, value) }

	val readerPageSwitch: Set<String>
		get() = prefs.getStringSet(KEY_READER_SWITCHERS, null) ?: setOf(PAGE_SWITCH_TAPS)

	var isTrafficWarningEnabled: Boolean
		get() = prefs.getBoolean(KEY_TRAFFIC_WARNING, true)
		set(value) = prefs.edit { putBoolean(KEY_TRAFFIC_WARNING, value) }

	var isAllFavouritesVisible: Boolean
		get() = prefs.getBoolean(KEY_ALL_FAVOURITES_VISIBLE, true)
		set(value) = prefs.edit { putBoolean(KEY_ALL_FAVOURITES_VISIBLE, value) }

	val isUpdateCheckingEnabled: Boolean
		get() = prefs.getBoolean(KEY_APP_UPDATE_AUTO, true)

	var lastUpdateCheckTimestamp: Long
		get() = prefs.getLong(KEY_APP_UPDATE, 0L)
		set(value) = prefs.edit { putLong(KEY_APP_UPDATE, value) }

	val isTrackerEnabled: Boolean
		get() = prefs.getBoolean(KEY_TRACKER_ENABLED, true)

	val isTrackerNotificationsEnabled: Boolean
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

	val defaultReaderMode: ReaderMode
		get() = prefs.getEnumValue(KEY_READER_MODE, ReaderMode.STANDARD)

	val isReaderModeDetectionEnabled: Boolean
		get() = prefs.getBoolean(KEY_READER_MODE_DETECT, true)

	var isHistoryGroupingEnabled: Boolean
		get() = prefs.getBoolean(KEY_HISTORY_GROUPING, true)
		set(value) = prefs.edit { putBoolean(KEY_HISTORY_GROUPING, value) }

	val isReadingIndicatorsEnabled: Boolean
		get() = prefs.getBoolean(KEY_READING_INDICATORS, true)

	val isHistoryExcludeNsfw: Boolean
		get() = prefs.getBoolean(KEY_HISTORY_EXCLUDE_NSFW, false)

	var isIncognitoModeEnabled: Boolean
		get() = prefs.getBoolean(KEY_INCOGNITO_MODE, false)
		set(value) = prefs.edit { putBoolean(KEY_INCOGNITO_MODE, value) }

	var chaptersReverse: Boolean
		get() = prefs.getBoolean(KEY_REVERSE_CHAPTERS, false)
		set(value) = prefs.edit { putBoolean(KEY_REVERSE_CHAPTERS, value) }

	val zoomMode: ZoomMode
		get() = prefs.getEnumValue(KEY_ZOOM_MODE, ZoomMode.FIT_CENTER)

	val trackSources: Set<String>
		get() = prefs.getStringSet(KEY_TRACK_SOURCES, null) ?: arraySetOf(TRACK_FAVOURITES, TRACK_HISTORY)

	var appPassword: String?
		get() = prefs.getString(KEY_APP_PASSWORD, null)
		set(value) = prefs.edit { if (value != null) putString(KEY_APP_PASSWORD, value) else remove(KEY_APP_PASSWORD) }

	var isBiometricProtectionEnabled: Boolean
		get() = prefs.getBoolean(KEY_PROTECT_APP_BIOMETRIC, true)
		set(value) = prefs.edit { putBoolean(KEY_PROTECT_APP_BIOMETRIC, value) }

	val isExitConfirmationEnabled: Boolean
		get() = prefs.getBoolean(KEY_EXIT_CONFIRM, false)

	var sourcesOrder: List<String>
		get() = prefs.getString(KEY_SOURCES_ORDER, null)
			?.split('|')
			.orEmpty()
		set(value) = prefs.edit {
			putString(KEY_SOURCES_ORDER, value.joinToString("|"))
		}

	var hiddenSources: Set<String>
		get() = prefs.getStringSet(KEY_SOURCES_HIDDEN, null) ?: emptySet()
		set(value) = prefs.edit { putStringSet(KEY_SOURCES_HIDDEN, value) }

	val isSourcesSelected: Boolean
		get() = KEY_SOURCES_HIDDEN in prefs

	val newSources: Set<MangaSource>
		get() {
			val known = sourcesOrder.toSet()
			val hidden = hiddenSources
			return remoteMangaSources
				.filterNotTo(EnumSet.noneOf(MangaSource::class.java)) { x ->
					x.name in known || x.name in hidden
				}
		}

	fun markKnownSources(sources: Collection<MangaSource>) {
		sourcesOrder = (sourcesOrder + sources.map { it.name }).distinct()
	}

	val isPagesNumbersEnabled: Boolean
		get() = prefs.getBoolean(KEY_PAGES_NUMBERS, false)

	val screenshotsPolicy: ScreenshotsPolicy
		get() = runCatching {
			val key = prefs.getString(KEY_SCREENSHOTS_POLICY, null)?.uppercase(Locale.ROOT)
			if (key == null) ScreenshotsPolicy.ALLOW else ScreenshotsPolicy.valueOf(key)
		}.getOrDefault(ScreenshotsPolicy.ALLOW)

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

	val isDownloadsSlowdownEnabled: Boolean
		get() = prefs.getBoolean(KEY_DOWNLOADS_SLOWDOWN, false)

	val downloadsParallelism: Int
		get() = prefs.getInt(KEY_DOWNLOADS_PARALLELISM, 2)

	val isSuggestionsEnabled: Boolean
		get() = prefs.getBoolean(KEY_SUGGESTIONS, false)

	val isSuggestionsExcludeNsfw: Boolean
		get() = prefs.getBoolean(KEY_SUGGESTIONS_EXCLUDE_NSFW, false)

	val dnsOverHttps: DoHProvider
		get() = prefs.getEnumValue(KEY_DOH, DoHProvider.NONE)

	fun isPagesPreloadAllowed(cm: ConnectivityManager): Boolean {
		return when (prefs.getString(KEY_PAGES_PRELOAD, null)?.toIntOrNull()) {
			NETWORK_ALWAYS -> true
			NETWORK_NEVER -> false
			else -> cm.isActiveNetworkMetered
		}
	}

	fun getDateFormat(format: String = prefs.getString(KEY_DATE_FORMAT, "").orEmpty()): DateFormat =
		when (format) {
			"" -> DateFormat.getDateInstance(DateFormat.SHORT)
			else -> SimpleDateFormat(format, Locale.getDefault())
		}

	fun getSuggestionsTagsBlacklistRegex(): Regex? {
		val string = prefs.getString(KEY_SUGGESTIONS_EXCLUDE_TAGS, null)?.trimEnd(' ', ',')
		if (string.isNullOrEmpty()) {
			return null
		}
		val tags = string.split(',')
		val regex = tags.joinToString(prefix = "(", separator = "|", postfix = ")") { tag ->
			Regex.escape(tag.trim())
		}
		return Regex(regex, RegexOption.IGNORE_CASE)
	}

	fun getMangaSources(includeHidden: Boolean): List<MangaSource> {
		val list = remoteSources.toMutableList()
		val order = sourcesOrder
		list.sortBy { x ->
			val e = order.indexOf(x.name)
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

	fun observe() = prefs.observe()

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
		const val KEY_SOURCES_ORDER = "sources_order_2"
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
		const val KEY_TRACKER_ENABLED = "tracker_enabled"
		const val KEY_TRACK_SOURCES = "track_sources"
		const val KEY_TRACK_CATEGORIES = "track_categories"
		const val KEY_TRACK_WARNING = "track_warning"
		const val KEY_TRACKER_NOTIFICATIONS = "tracker_notifications"
		const val KEY_NOTIFICATIONS_SETTINGS = "notifications_settings"
		const val KEY_NOTIFICATIONS_SOUND = "notifications_sound"
		const val KEY_NOTIFICATIONS_VIBRATE = "notifications_vibrate"
		const val KEY_NOTIFICATIONS_LIGHT = "notifications_light"
		const val KEY_NOTIFICATIONS_INFO = "tracker_notifications_info"
		const val KEY_READER_ANIMATION = "reader_animation"
		const val KEY_READER_MODE = "reader_mode"
		const val KEY_READER_MODE_DETECT = "reader_mode_detect"
		const val KEY_APP_PASSWORD = "app_password"
		const val KEY_PROTECT_APP = "protect_app"
		const val KEY_PROTECT_APP_BIOMETRIC = "protect_app_bio"
		const val KEY_APP_VERSION = "app_version"
		const val KEY_ZOOM_MODE = "zoom_mode"
		const val KEY_BACKUP = "backup"
		const val KEY_RESTORE = "restore"
		const val KEY_HISTORY_GROUPING = "history_grouping"
		const val KEY_READING_INDICATORS = "reading_indicators"
		const val KEY_REVERSE_CHAPTERS = "reverse_chapters"
		const val KEY_HISTORY_EXCLUDE_NSFW = "history_exclude_nsfw"
		const val KEY_PAGES_NUMBERS = "pages_numbers"
		const val KEY_SCREENSHOTS_POLICY = "screenshots_policy"
		const val KEY_PAGES_PRELOAD = "pages_preload"
		const val KEY_SUGGESTIONS = "suggestions"
		const val KEY_SUGGESTIONS_EXCLUDE_NSFW = "suggestions_exclude_nsfw"
		const val KEY_SUGGESTIONS_EXCLUDE_TAGS = "suggestions_exclude_tags"
		const val KEY_SHIKIMORI = "shikimori"
		const val KEY_DOWNLOADS_PARALLELISM = "downloads_parallelism"
		const val KEY_DOWNLOADS_SLOWDOWN = "downloads_slowdown"
		const val KEY_ALL_FAVOURITES_VISIBLE = "all_favourites_visible"
		const val KEY_DOH = "doh"
		const val KEY_EXIT_CONFIRM = "exit_confirm"
		const val KEY_INCOGNITO_MODE = "incognito"
		const val KEY_SYNC = "sync"

		// About
		const val KEY_APP_UPDATE = "app_update"
		const val KEY_APP_UPDATE_AUTO = "app_update_auto"
		const val KEY_APP_TRANSLATION = "about_app_translation"

		private const val NETWORK_NEVER = 0
		private const val NETWORK_ALWAYS = 1
		private const val NETWORK_NON_METERED = 2
	}
}
