package org.koitharu.kotatsu.core.prefs

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.FloatRange
import androidx.appcompat.app.AppCompatDelegate
import androidx.collection.ArraySet
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.koitharu.kotatsu.core.model.ZoomMode
import org.koitharu.kotatsu.core.network.DoHProvider
import org.koitharu.kotatsu.core.util.ext.connectivityManager
import org.koitharu.kotatsu.core.util.ext.getEnumValue
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.putEnumValue
import org.koitharu.kotatsu.core.util.ext.takeIfReadable
import org.koitharu.kotatsu.core.util.ext.toUriOrNull
import org.koitharu.kotatsu.history.domain.model.HistoryOrder
import org.koitharu.kotatsu.parsers.model.SortOrder
import org.koitharu.kotatsu.parsers.util.find
import org.koitharu.kotatsu.parsers.util.mapNotNullToSet
import org.koitharu.kotatsu.parsers.util.mapToSet
import java.io.File
import java.net.Proxy
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSettings @Inject constructor(@ApplicationContext context: Context) {

	private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
	private val connectivityManager = context.connectivityManager

	var listMode: ListMode
		get() = prefs.getEnumValue(KEY_LIST_MODE, ListMode.GRID)
		set(value) = prefs.edit { putEnumValue(KEY_LIST_MODE, value) }

	val theme: Int
		get() = prefs.getString(KEY_THEME, null)?.toIntOrNull()
			?: AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM

	val colorScheme: ColorScheme
		get() = prefs.getEnumValue(KEY_COLOR_THEME, ColorScheme.default)

	val isAmoledTheme: Boolean
		get() = prefs.getBoolean(KEY_THEME_AMOLED, false)

	var mainNavItems: List<NavItem>
		get() {
			val raw = prefs.getString(KEY_NAV_MAIN, null)?.split(',')
			return if (raw.isNullOrEmpty()) {
				listOf(NavItem.HISTORY, NavItem.FAVORITES, NavItem.EXPLORE, NavItem.FEED)
			} else {
				raw.mapNotNull { x -> NavItem.entries.find(x) }.ifEmpty { listOf(NavItem.EXPLORE) }
			}
		}
		set(value) {
			prefs.edit {
				putString(KEY_NAV_MAIN, value.joinToString(",") { it.name })
			}
		}

	var gridSize: Int
		get() = prefs.getInt(KEY_GRID_SIZE, 100)
		set(value) = prefs.edit { putInt(KEY_GRID_SIZE, value) }

	var isNsfwContentDisabled: Boolean
		get() = prefs.getBoolean(KEY_DISABLE_NSFW, false)
		set(value) = prefs.edit { putBoolean(KEY_DISABLE_NSFW, value) }

	var appLocales: LocaleListCompat
		get() {
			val raw = prefs.getString(KEY_APP_LOCALE, null)
			return LocaleListCompat.forLanguageTags(raw)
		}
		set(value) {
			prefs.edit {
				putString(KEY_APP_LOCALE, value.toLanguageTags())
			}
		}

	val readerPageSwitch: Set<String>
		get() = prefs.getStringSet(KEY_READER_SWITCHERS, null) ?: setOf(PAGE_SWITCH_TAPS)

	val isReaderZoomButtonsEnabled: Boolean
		get() = prefs.getBoolean(KEY_READER_ZOOM_BUTTONS, false)

	val isReaderTapsAdaptive: Boolean
		get() = !prefs.getBoolean(KEY_READER_TAPS_LTR, false)

	var isTrafficWarningEnabled: Boolean
		get() = prefs.getBoolean(KEY_TRAFFIC_WARNING, true)
		set(value) = prefs.edit { putBoolean(KEY_TRAFFIC_WARNING, value) }

	var isAllFavouritesVisible: Boolean
		get() = prefs.getBoolean(KEY_ALL_FAVOURITES_VISIBLE, true)
		set(value) = prefs.edit { putBoolean(KEY_ALL_FAVOURITES_VISIBLE, value) }

	val isTrackerEnabled: Boolean
		get() = prefs.getBoolean(KEY_TRACKER_ENABLED, true)

	val isTrackerWifiOnly: Boolean
		get() = prefs.getBoolean(KEY_TRACKER_WIFI_ONLY, false)

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

	val readerAnimation: ReaderAnimation
		get() = prefs.getEnumValue(KEY_READER_ANIMATION, ReaderAnimation.DEFAULT)

	val readerBackground: ReaderBackground
		get() = prefs.getEnumValue(KEY_READER_BACKGROUND, ReaderBackground.DEFAULT)

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
		get() = prefs.getStringSet(KEY_TRACK_SOURCES, null) ?: setOf(TRACK_FAVOURITES)

	var appPassword: String?
		get() = prefs.getString(KEY_APP_PASSWORD, null)
		set(value) = prefs.edit {
			if (value != null) putString(KEY_APP_PASSWORD, value) else remove(
				KEY_APP_PASSWORD,
			)
		}

	val isLoggingEnabled: Boolean
		get() = prefs.getBoolean(KEY_LOGGING_ENABLED, false)

	var isBiometricProtectionEnabled: Boolean
		get() = prefs.getBoolean(KEY_PROTECT_APP_BIOMETRIC, true)
		set(value) = prefs.edit { putBoolean(KEY_PROTECT_APP_BIOMETRIC, value) }

	val isMirrorSwitchingAvailable: Boolean
		get() = prefs.getBoolean(KEY_MIRROR_SWITCHING, true)

	val isExitConfirmationEnabled: Boolean
		get() = prefs.getBoolean(KEY_EXIT_CONFIRM, false)

	val isDynamicShortcutsEnabled: Boolean
		get() = prefs.getBoolean(KEY_SHORTCUTS, true)

	val isUnstableUpdatesAllowed: Boolean
		get() = prefs.getBoolean(KEY_UPDATES_UNSTABLE, false)

	val isContentPrefetchEnabled: Boolean
		get() {
			if (isBackgroundNetworkRestricted()) {
				return false
			}
			val policy =
				NetworkPolicy.from(prefs.getString(KEY_PREFETCH_CONTENT, null), NetworkPolicy.NEVER)
			return policy.isNetworkAllowed(connectivityManager)
		}

	var isSourcesGridMode: Boolean
		get() = prefs.getBoolean(KEY_SOURCES_GRID, false)
		set(value) = prefs.edit { putBoolean(KEY_SOURCES_GRID, value) }

	val isPagesNumbersEnabled: Boolean
		get() = prefs.getBoolean(KEY_PAGES_NUMBERS, false)

	val screenshotsPolicy: ScreenshotsPolicy
		get() = runCatching {
			val key = prefs.getString(KEY_SCREENSHOTS_POLICY, null)?.uppercase(Locale.ROOT)
			if (key == null) ScreenshotsPolicy.ALLOW else ScreenshotsPolicy.valueOf(key)
		}.getOrDefault(ScreenshotsPolicy.ALLOW)

	var userSpecifiedMangaDirectories: Set<File>
		get() {
			val set = prefs.getStringSet(KEY_LOCAL_MANGA_DIRS, emptySet()).orEmpty()
			return set.mapNotNullToSet { File(it).takeIfReadable() }
		}
		set(value) {
			val set = value.mapToSet { it.absolutePath }
			prefs.edit { putStringSet(KEY_LOCAL_MANGA_DIRS, set) }
		}

	var mangaStorageDir: File?
		get() = prefs.getString(KEY_LOCAL_STORAGE, null)?.let {
			File(it)
		}?.takeIf { it.exists() && it in userSpecifiedMangaDirectories }
		set(value) = prefs.edit {
			if (value == null) {
				remove(KEY_LOCAL_STORAGE)
			} else {
				val userDirs = userSpecifiedMangaDirectories
				if (value !in userDirs) {
					userSpecifiedMangaDirectories = userDirs + value
				}
				putString(KEY_LOCAL_STORAGE, value.path)
			}
		}

	val isDownloadsSlowdownEnabled: Boolean
		get() = prefs.getBoolean(KEY_DOWNLOADS_SLOWDOWN, false)

	val isDownloadsWiFiOnly: Boolean
		get() = prefs.getBoolean(KEY_DOWNLOADS_WIFI, false)

	var isSuggestionsEnabled: Boolean
		get() = prefs.getBoolean(KEY_SUGGESTIONS, false)
		set(value) = prefs.edit { putBoolean(KEY_SUGGESTIONS, value) }

	val isSuggestionsWiFiOnly: Boolean
		get() = prefs.getBoolean(KEY_SUGGESTIONS_WIFI_ONLY, false)

	val isSuggestionsExcludeNsfw: Boolean
		get() = prefs.getBoolean(KEY_SUGGESTIONS_EXCLUDE_NSFW, false)

	val isSuggestionsNotificationAvailable: Boolean
		get() = prefs.getBoolean(KEY_SUGGESTIONS_NOTIFICATIONS, false)

	val suggestionsTagsBlacklist: Set<String>
		get() {
			val string = prefs.getString(KEY_SUGGESTIONS_EXCLUDE_TAGS, null)?.trimEnd(' ', ',')
			if (string.isNullOrEmpty()) {
				return emptySet()
			}
			return string.split(',').mapToSet { it.trim() }
		}

	val isReaderBarEnabled: Boolean
		get() = prefs.getBoolean(KEY_READER_BAR, true)

	val isReaderSliderEnabled: Boolean
		get() = prefs.getBoolean(KEY_READER_SLIDER, true)

	val isImagesProxyEnabled: Boolean
		get() = prefs.getBoolean(KEY_IMAGES_PROXY, false)

	val dnsOverHttps: DoHProvider
		get() = prefs.getEnumValue(KEY_DOH, DoHProvider.NONE)

	val isSSLBypassEnabled: Boolean
		get() = prefs.getBoolean(KEY_SSL_BYPASS, false)

	val proxyType: Proxy.Type
		get() {
			val raw = prefs.getString(KEY_PROXY_TYPE, null) ?: return Proxy.Type.DIRECT
			return enumValues<Proxy.Type>().find { it.name == raw } ?: Proxy.Type.DIRECT
		}

	val proxyAddress: String?
		get() = prefs.getString(KEY_PROXY_ADDRESS, null)

	val proxyPort: Int
		get() = prefs.getString(KEY_PROXY_PORT, null)?.toIntOrNull() ?: 0

	val proxyLogin: String?
		get() = prefs.getString(KEY_PROXY_LOGIN, null)?.takeUnless { it.isEmpty() }

	val proxyPassword: String?
		get() = prefs.getString(KEY_PROXY_PASSWORD, null)?.takeUnless { it.isEmpty() }

	var localListOrder: SortOrder
		get() = prefs.getEnumValue(KEY_LOCAL_LIST_ORDER, SortOrder.NEWEST)
		set(value) = prefs.edit { putEnumValue(KEY_LOCAL_LIST_ORDER, value) }

	var historySortOrder: HistoryOrder
		get() = prefs.getEnumValue(KEY_HISTORY_ORDER, HistoryOrder.UPDATED)
		set(value) = prefs.edit { putEnumValue(KEY_HISTORY_ORDER, value) }

	val isRelatedMangaEnabled: Boolean
		get() = prefs.getBoolean(KEY_RELATED_MANGA, true)

	val isWebtoonZoomEnable: Boolean
		get() = prefs.getBoolean(KEY_WEBTOON_ZOOM, true)

	@get:FloatRange(from = 0.0, to = 1.0)
	var readerAutoscrollSpeed: Float
		get() = prefs.getFloat(KEY_READER_AUTOSCROLL_SPEED, 0f)
		set(@FloatRange(from = 0.0, to = 1.0) value) = prefs.edit {
			putFloat(
				KEY_READER_AUTOSCROLL_SPEED,
				value,
			)
		}

	val isPagesPreloadEnabled: Boolean
		get() {
			if (isBackgroundNetworkRestricted()) {
				return false
			}
			val policy = NetworkPolicy.from(
				prefs.getString(KEY_PAGES_PRELOAD, null),
				NetworkPolicy.NON_METERED,
			)
			return policy.isNetworkAllowed(connectivityManager)
		}

	fun isTipEnabled(tip: String): Boolean {
		return prefs.getStringSet(KEY_TIPS_CLOSED, emptySet())?.contains(tip) != true
	}

	fun closeTip(tip: String) {
		val closedTips = prefs.getStringSet(KEY_TIPS_CLOSED, emptySet()).orEmpty()
		if (tip in closedTips) {
			return
		}
		prefs.edit { putStringSet(KEY_TIPS_CLOSED, closedTips + tip) }
	}

	fun subscribe(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
		prefs.registerOnSharedPreferenceChangeListener(listener)
	}

	fun unsubscribe(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
		prefs.unregisterOnSharedPreferenceChangeListener(listener)
	}

	fun observe() = prefs.observe()

	fun getAllValues(): Map<String, *> = prefs.all

	fun upsertAll(m: Map<String, *>) {
		prefs.edit {
			m.forEach { e ->
				when (val v = e.value) {
					is Boolean -> putBoolean(e.key, v)
					is Int -> putInt(e.key, v)
					is Long -> putLong(e.key, v)
					is Float -> putFloat(e.key, v)
					is String -> putString(e.key, v)
					is JSONArray -> putStringSet(e.key, v.toStringSet())
				}
			}
		}
	}

	private fun isBackgroundNetworkRestricted(): Boolean {
		return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			connectivityManager.restrictBackgroundStatus == ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED
		} else {
			false
		}
	}

	private fun JSONArray.toStringSet(): Set<String> {
		val len = length()
		val result = ArraySet<String>(len)
		for (i in 0 until len) {
			result.add(getString(i))
		}
		return result
	}

	companion object {

		const val PAGE_SWITCH_TAPS = "taps"
		const val PAGE_SWITCH_VOLUME_KEYS = "volume"

		const val TRACK_HISTORY = "history"
		const val TRACK_FAVOURITES = "favourites"

		const val KEY_LIST_MODE = "list_mode_2"
		const val KEY_THEME = "theme"
		const val KEY_COLOR_THEME = "color_theme"
		const val KEY_THEME_AMOLED = "amoled_theme"
		const val KEY_TRAFFIC_WARNING = "traffic_warning"
		const val KEY_PAGES_CACHE_CLEAR = "pages_cache_clear"
		const val KEY_HTTP_CACHE_CLEAR = "http_cache_clear"
		const val KEY_COOKIES_CLEAR = "cookies_clear"
		const val KEY_THUMBS_CACHE_CLEAR = "thumbs_cache_clear"
		const val KEY_SEARCH_HISTORY_CLEAR = "search_history_clear"
		const val KEY_UPDATES_FEED_CLEAR = "updates_feed_clear"
		const val KEY_GRID_SIZE = "grid_size"
		const val KEY_REMOTE_SOURCES = "remote_sources"
		const val KEY_LOCAL_STORAGE = "local_storage"
		const val KEY_READER_SWITCHERS = "reader_switchers"
		const val KEY_READER_ZOOM_BUTTONS = "reader_zoom_buttons"
		const val KEY_TRACKER_ENABLED = "tracker_enabled"
		const val KEY_TRACKER_WIFI_ONLY = "tracker_wifi"
		const val KEY_TRACK_SOURCES = "track_sources"
		const val KEY_TRACK_CATEGORIES = "track_categories"
		const val KEY_TRACK_WARNING = "track_warning"
		const val KEY_TRACKER_NOTIFICATIONS = "tracker_notifications"
		const val KEY_NOTIFICATIONS_SETTINGS = "notifications_settings"
		const val KEY_NOTIFICATIONS_SOUND = "notifications_sound"
		const val KEY_NOTIFICATIONS_VIBRATE = "notifications_vibrate"
		const val KEY_NOTIFICATIONS_LIGHT = "notifications_light"
		const val KEY_NOTIFICATIONS_INFO = "tracker_notifications_info"
		const val KEY_READER_ANIMATION = "reader_animation2"
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
		const val KEY_SUGGESTIONS_WIFI_ONLY = "suggestions_wifi"
		const val KEY_SUGGESTIONS_EXCLUDE_NSFW = "suggestions_exclude_nsfw"
		const val KEY_SUGGESTIONS_EXCLUDE_TAGS = "suggestions_exclude_tags"
		const val KEY_SUGGESTIONS_NOTIFICATIONS = "suggestions_notifications"
		const val KEY_SHIKIMORI = "shikimori"
		const val KEY_ANILIST = "anilist"
		const val KEY_MAL = "mal"
		const val KEY_DOWNLOADS_SLOWDOWN = "downloads_slowdown"
		const val KEY_DOWNLOADS_WIFI = "downloads_wifi"
		const val KEY_ALL_FAVOURITES_VISIBLE = "all_favourites_visible"
		const val KEY_DOH = "doh"
		const val KEY_EXIT_CONFIRM = "exit_confirm"
		const val KEY_INCOGNITO_MODE = "incognito"
		const val KEY_SYNC = "sync"
		const val KEY_SYNC_SETTINGS = "sync_settings"
		const val KEY_READER_BAR = "reader_bar"
		const val KEY_READER_SLIDER = "reader_slider"
		const val KEY_READER_BACKGROUND = "reader_background"
		const val KEY_SHORTCUTS = "dynamic_shortcuts"
		const val KEY_READER_TAPS_LTR = "reader_taps_ltr"
		const val KEY_LOCAL_LIST_ORDER = "local_order"
		const val KEY_HISTORY_ORDER = "history_order"
		const val KEY_WEBTOON_ZOOM = "webtoon_zoom"
		const val KEY_PREFETCH_CONTENT = "prefetch_content"
		const val KEY_APP_LOCALE = "app_locale"
		const val KEY_LOGGING_ENABLED = "logging"
		const val KEY_LOGS_SHARE = "logs_share"
		const val KEY_SOURCES_GRID = "sources_grid"
		const val KEY_UPDATES_UNSTABLE = "updates_unstable"
		const val KEY_TIPS_CLOSED = "tips_closed"
		const val KEY_SSL_BYPASS = "ssl_bypass"
		const val KEY_READER_AUTOSCROLL_SPEED = "as_speed"
		const val KEY_MIRROR_SWITCHING = "mirror_switching"
		const val KEY_PROXY = "proxy"
		const val KEY_PROXY_TYPE = "proxy_type"
		const val KEY_PROXY_ADDRESS = "proxy_address"
		const val KEY_PROXY_PORT = "proxy_port"
		const val KEY_PROXY_AUTH = "proxy_auth"
		const val KEY_PROXY_LOGIN = "proxy_login"
		const val KEY_PROXY_PASSWORD = "proxy_password"
		const val KEY_IMAGES_PROXY = "images_proxy"
		const val KEY_LOCAL_MANGA_DIRS = "local_manga_dirs"
		const val KEY_DISABLE_NSFW = "no_nsfw"
		const val KEY_RELATED_MANGA = "related_manga"
		const val KEY_NAV_MAIN = "nav_main"

		// About
		const val KEY_APP_UPDATE = "app_update"
		const val KEY_APP_TRANSLATION = "about_app_translation"
	}
}
