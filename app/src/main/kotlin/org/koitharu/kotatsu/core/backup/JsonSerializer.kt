package org.koitharu.kotatsu.core.backup

import org.json.JSONArray
import org.json.JSONObject
import org.koitharu.kotatsu.core.db.entity.MangaEntity
import org.koitharu.kotatsu.core.db.entity.TagEntity
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.favourites.data.FavouriteCategoryEntity
import org.koitharu.kotatsu.favourites.data.FavouriteEntity
import org.koitharu.kotatsu.history.data.HistoryEntity
import java.util.ArrayList

class JsonSerializer private constructor(private val json: JSONObject) {

	constructor(e: FavouriteEntity) : this(
		JSONObject().apply {
			put("manga_id", e.mangaId)
			put("category_id", e.categoryId)
			put("sort_key", e.sortKey)
			put("created_at", e.createdAt)
		},
	)

	constructor(e: FavouriteCategoryEntity) : this(
		JSONObject().apply {
			put("category_id", e.categoryId)
			put("created_at", e.createdAt)
			put("sort_key", e.sortKey)
			put("title", e.title)
			put("order", e.order)
			put("track", e.track)
			put("show_in_lib", e.isVisibleInLibrary)
		},
	)

	constructor(e: HistoryEntity) : this(
		JSONObject().apply {
			put("manga_id", e.mangaId)
			put("created_at", e.createdAt)
			put("updated_at", e.updatedAt)
			put("chapter_id", e.chapterId)
			put("page", e.page)
			put("scroll", e.scroll)
			put("percent", e.percent)
		},
	)

	constructor(e: TagEntity) : this(
		JSONObject().apply {
			put("id", e.id)
			put("title", e.title)
			put("key", e.key)
			put("source", e.source)
		},
	)

	constructor(e: MangaEntity) : this(
		JSONObject().apply {
			put("id", e.id)
			put("title", e.title)
			put("alt_title", e.altTitle)
			put("url", e.url)
			put("public_url", e.publicUrl)
			put("rating", e.rating)
			put("nsfw", e.isNsfw)
			put("cover_url", e.coverUrl)
			put("large_cover_url", e.largeCoverUrl)
			put("state", e.state)
			put("author", e.author)
			put("source", e.source)
		},
	)

	constructor(e: AppSettings) : this(
		JSONObject().apply {
			put("list_mode", e.listMode.name)
			put("theme", e.theme)
			put("color_scheme", e.colorScheme.name)
			put("is_amoled_theme", e.isAmoledTheme)
			put("grid_size", e.gridSize)
			put("reader_page_switch", JSONArray(e.readerPageSwitch))
			put("is_reader_taps_adaptive", e.isReaderTapsAdaptive)
			put("is_traffic_waring_enabled", e.isTrafficWarningEnabled)
			put("is_all_favourites_visible", e.isAllFavouritesVisible)
			put("is_tracker_enabled", e.isTrackerEnabled)
			put("is_tracker_notifications_enabled", e.isTrackerNotificationsEnabled)
			put("notification_sound", e.notificationSound.toString())
			put("notification_vibrate", e.notificationVibrate)
			put("notification_light", e.notificationLight)
			put("reader_animation", e.readerAnimation)
			put("default_reader_node", e.defaultReaderMode.name)
			put("is_reader_mode_detection_enabled", e.isReaderModeDetectionEnabled)
			put("is_history_grouping_enabled", e.isHistoryGroupingEnabled)
			put("is_reading_indicators_enabled", e.isReadingIndicatorsEnabled)
			put("is_history_exclude_nsfw", e.isHistoryExcludeNsfw)
			put("is_incognito_mode_enabled", e.isIncognitoModeEnabled) // maybe we should omit this
			put("chapters_reverse", e.chaptersReverse)
			put("zoom_mode", e.zoomMode)
			put("track_sources", JSONArray(e.trackSources))
			put("is_logging_enabled", e.isLoggingEnabled)
			put("is_mirror_switching_available", e.isMirrorSwitchingAvailable)
			put("is_exit_confirmation_enabled", e.isExitConfirmationEnabled)
			put("is_dynamic_shortcuts_enabled", e.isDynamicShortcutsEnabled)
			put("is_unstable_updates_allowed", e.isUnstableUpdatesAllowed)
			put("sources_order", JSONArray(e.sourcesOrder))
			put("hidden_sources", JSONArray(e.hiddenSources))
			put("is_sources_grid_mode", e.isSourcesGridMode)
			put(
				"user_specified_manga_directions",
				JSONArray(e.userSpecifiedMangaDirectories.map { it.absolutePath }),
			)
			put("manga_storage_dir", e.mangaStorageDir?.absolutePath)
			put("is_downloads_slowdown_enabled", e.isDownloadsSlowdownEnabled)
			put("is_downloads_wifi_only", e.isDownloadsWiFiOnly)
			put("is_suggestions_enabled", e.isSuggestionsEnabled)
			put("is_suggestions_exclude_nsfw", e.isSuggestionsExcludeNsfw)
			put("is_suggestions_notification_available", e.isSuggestionsNotificationAvailable)
			put("suggestions_tags_blacklist", JSONArray(e.suggestionsTagsBlacklist))
			put("is_reader_bar_enabled", e.isReaderBarEnabled)
			put("is_reader_slider_enabled", e.isReaderSliderEnabled)
			put("is_images_proxy_enabled", e.isImagesProxyEnabled)
			put("dns_over_https", e.dnsOverHttps.name)
			put("is_ssl_bypass_enabled", e.isSSLBypassEnabled)
			put("local_list_order", e.localListOrder.name)
			put("is_webtoon_zoom_enabled", e.isWebtoonZoomEnable)
			put("reader_autoscroll_speed", e.readerAutoscrollSpeed)
		},
	)

	fun toJson(): JSONObject = json
}


// I have copied these extension functions from parser library,
// because, library doesn't support mapping primitive types (string, int, float ...),
// I didn't know where to put this extension functions :(

inline fun <K, T> JSONArray.mapJSONToArray(
	block: (K) -> T,
): List<T> {
	val len = length()
	val result = ArrayList<T>(len)
	for (i in 0 until len) {
		val jo = get(i) as K
		result.add(block(jo))
	}
	return result
}

fun <K, T> JSONArray.mapJSONToSet(block: (K) -> T): Set<T> {
	val len = length()
	val result = androidx.collection.ArraySet<T>(len)
	for (i in 0 until len) {
		val jo = get(i) as K
		result.add(block(jo))
	}
	return result
}
