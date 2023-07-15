package org.koitharu.kotatsu.core.backup

import android.provider.Settings
import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.model.ZoomMode
import org.koitharu.kotatsu.core.network.DoHProvider
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ColorScheme
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.core.prefs.ReaderMode
import org.koitharu.kotatsu.core.util.ext.getEnumValue
import org.koitharu.kotatsu.core.util.ext.takeIfReadable
import org.koitharu.kotatsu.core.util.ext.toUriOrNull
import org.koitharu.kotatsu.parsers.model.SortOrder
import org.koitharu.kotatsu.parsers.util.json.JSONIterator
import org.koitharu.kotatsu.parsers.util.json.getFloatOrDefault
import org.koitharu.kotatsu.parsers.util.json.getStringOrNull
import org.koitharu.kotatsu.parsers.util.json.mapJSON
import org.koitharu.kotatsu.parsers.util.mapNotNullToSet
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import java.io.File
import javax.inject.Inject

private const val PAGE_SIZE = 10

class BackupRepository @Inject constructor(
	private val db: MangaDatabase,
	private val settings: AppSettings,
) {

	suspend fun dumpHistory(): BackupEntry {
		var offset = 0
		val entry = BackupEntry(BackupEntry.HISTORY, JSONArray())
		while (true) {
			val history = db.historyDao.findAll(offset, PAGE_SIZE)
			if (history.isEmpty()) {
				break
			}
			offset += history.size
			for (item in history) {
				val manga = JsonSerializer(item.manga).toJson()
				val tags = JSONArray()
				item.tags.forEach { tags.put(JsonSerializer(it).toJson()) }
				manga.put("tags", tags)
				val json = JsonSerializer(item.history).toJson()
				json.put("manga", manga)
				entry.data.put(json)
			}
		}
		return entry
	}

	suspend fun dumpCategories(): BackupEntry {
		val entry = BackupEntry(BackupEntry.CATEGORIES, JSONArray())
		val categories = db.favouriteCategoriesDao.findAll()
		for (item in categories) {
			entry.data.put(JsonSerializer(item).toJson())
		}
		return entry
	}

	suspend fun dumpFavourites(): BackupEntry {
		var offset = 0
		val entry = BackupEntry(BackupEntry.FAVOURITES, JSONArray())
		while (true) {
			val favourites = db.favouritesDao.findAll(offset, PAGE_SIZE)
			if (favourites.isEmpty()) {
				break
			}
			offset += favourites.size
			for (item in favourites) {
				val manga = JsonSerializer(item.manga).toJson()
				val tags = JSONArray()
				item.tags.forEach { tags.put(JsonSerializer(it).toJson()) }
				manga.put("tags", tags)
				val json = JsonSerializer(item.favourite).toJson()
				json.put("manga", manga)
				entry.data.put(json)
			}
		}
		return entry
	}

	suspend fun dumpSettings(): BackupEntry {
		val entry = BackupEntry(BackupEntry.SETTINGS, JSONArray())
		val json = JsonSerializer(settings).toJson()
		entry.data.put(json)
		return entry
	}

	fun createIndex(): BackupEntry {
		val entry = BackupEntry(BackupEntry.INDEX, JSONArray())
		val json = JSONObject()
		json.put("app_id", BuildConfig.APPLICATION_ID)
		json.put("app_version", BuildConfig.VERSION_CODE)
		json.put("created_at", System.currentTimeMillis())
		entry.data.put(json)
		return entry
	}

	suspend fun restoreHistory(entry: BackupEntry): CompositeResult {
		val result = CompositeResult()
		for (item in entry.data.JSONIterator()) {
			val mangaJson = item.getJSONObject("manga")
			val manga = JsonDeserializer(mangaJson).toMangaEntity()
			val tags = mangaJson.getJSONArray("tags").mapJSON {
				JsonDeserializer(it).toTagEntity()
			}
			val history = JsonDeserializer(item).toHistoryEntity()
			result += runCatchingCancellable {
				db.withTransaction {
					db.tagsDao.upsert(tags)
					db.mangaDao.upsert(manga, tags)
					db.historyDao.upsert(history)
				}
			}
		}
		return result
	}

	suspend fun restoreCategories(entry: BackupEntry): CompositeResult {
		val result = CompositeResult()
		for (item in entry.data.JSONIterator()) {
			val category = JsonDeserializer(item).toFavouriteCategoryEntity()
			result += runCatchingCancellable {
				db.favouriteCategoriesDao.upsert(category)
			}
		}
		return result
	}

	suspend fun restoreFavourites(entry: BackupEntry): CompositeResult {
		val result = CompositeResult()
		for (item in entry.data.JSONIterator()) {
			val mangaJson = item.getJSONObject("manga")
			val manga = JsonDeserializer(mangaJson).toMangaEntity()
			val tags = mangaJson.getJSONArray("tags").mapJSON {
				JsonDeserializer(it).toTagEntity()
			}
			val favourite = JsonDeserializer(item).toFavouriteEntity()
			result += runCatchingCancellable {
				db.withTransaction {
					db.tagsDao.upsert(tags)
					db.mangaDao.upsert(manga, tags)
					db.favouritesDao.upsert(favourite)
				}
			}
		}
		return result
	}

	fun restoreSettings(entry: BackupEntry): CompositeResult {
		val result = CompositeResult()
		for (item in entry.data.JSONIterator()) {
			result += runCatchingCancellable {
				settings.listMode = item.getString("list_mode").getEnumValue(ListMode.GRID)
				settings.theme = item.getInt("theme")
				settings.colorScheme = item.getString("color_scheme").getEnumValue(ColorScheme.default)
				settings.isAmoledTheme = item.getBoolean("is_amoled_theme")
				settings.gridSize = item.getInt("grid_size")
				settings.readerPageSwitch =
					item.getJSONArray("reader_page_switch").mapJSONToSet<String, String> { it }
				settings.isReaderTapsAdaptive = item.getBoolean("is_reader_taps_adaptive")
				settings.isTrafficWarningEnabled = item.getBoolean("is_traffic_waring_enabled")
				settings.isAllFavouritesVisible = item.getBoolean("is_all_favourites_visible")
				settings.isTrackerEnabled = item.getBoolean("is_tracker_enabled")
				settings.isTrackerNotificationsEnabled = item.getBoolean("is_tracker_notifications_enabled")
				settings.notificationSound =
					item.getString("notification_sound").toUriOrNull() ?: Settings.System.DEFAULT_NOTIFICATION_URI
				settings.notificationVibrate = item.getBoolean("notification_vibrate")
				settings.notificationLight = item.getBoolean("notification_light")
				settings.readerAnimation = item.getBoolean("reader_animation")
				settings.defaultReaderMode = item.getString("default_reader_node").getEnumValue(ReaderMode.STANDARD)
				settings.isReaderModeDetectionEnabled = item.getBoolean("is_reader_mode_detection_enabled")
				settings.isHistoryGroupingEnabled = item.getBoolean("is_history_grouping_enabled")
				settings.isReadingIndicatorsEnabled = item.getBoolean("is_reading_indicators_enabled")
				settings.isHistoryExcludeNsfw = item.getBoolean("is_history_exclude_nsfw")
				settings.isIncognitoModeEnabled = item.getBoolean("is_incognito_mode_enabled")
				settings.chaptersReverse = item.getBoolean("chapters_reverse")
				settings.zoomMode = item.getString("zoom_mode").getEnumValue(ZoomMode.FIT_CENTER)
				settings.trackSources = item.getJSONArray("track_sources").mapJSONToSet<String, String> { it }
				settings.isLoggingEnabled = item.getBoolean("is_logging_enabled")
				settings.isMirrorSwitchingAvailable = item.getBoolean("is_mirror_switching_available")
				settings.isExitConfirmationEnabled = item.getBoolean("is_exit_confirmation_enabled")
				settings.isDynamicShortcutsEnabled = item.getBoolean("is_dynamic_shortcuts_enabled")
				settings.isUnstableUpdatesAllowed = item.getBoolean("is_unstable_updates_allowed")
				settings.sourcesOrder = item.getJSONArray("sources_order").mapJSONToArray<String, String> { it }
				settings.hiddenSources = item.getJSONArray("hidden_sources").mapJSONToSet<String, String> { it }
				settings.isSourcesGridMode = item.getBoolean("is_sources_grid_mode")
				settings.userSpecifiedMangaDirectories = item.getJSONArray("user_specified_manga_directions")
					.mapJSONToSet<String, String> { it }.mapNotNullToSet { File(it).takeIfReadable() }
				File(item.getStringOrNull("manga_storage_dir") ?: "").takeIfReadable()?.let {
					settings.mangaStorageDir = it
				}
				settings.isDownloadsSlowdownEnabled = item.getBoolean("is_downloads_slowdown_enabled")
				settings.isDownloadsWiFiOnly = item.getBoolean("is_downloads_wifi_only")
				settings.isSuggestionsEnabled = item.getBoolean("is_suggestions_enabled")
				settings.isSuggestionsExcludeNsfw = item.getBoolean("is_suggestions_exclude_nsfw")
				settings.isSuggestionsNotificationAvailable = item.getBoolean("is_suggestions_notification_available")
				settings.suggestionsTagsBlacklist =
					item.getJSONArray("suggestions_tags_blacklist").mapJSONToSet<String, String> { it }
				settings.isReaderBarEnabled = item.getBoolean("is_reader_bar_enabled")
				settings.isReaderSliderEnabled = item.getBoolean("is_reader_slider_enabled")
				settings.isImagesProxyEnabled = item.getBoolean("is_images_proxy_enabled")
				settings.dnsOverHttps = item.getString("dns_over_https").getEnumValue(DoHProvider.NONE)
				settings.isSSLBypassEnabled = item.getBoolean("is_ssl_bypass_enabled")
				settings.localListOrder = item.getString("local_list_order").getEnumValue(SortOrder.NEWEST)
				settings.isWebtoonZoomEnable = item.getBoolean("is_webtoon_zoom_enabled")
				settings.readerAutoscrollSpeed = item.getFloatOrDefault("reader_autoscroll_speed", 0f)
			}
		}
		return result
	}
}
