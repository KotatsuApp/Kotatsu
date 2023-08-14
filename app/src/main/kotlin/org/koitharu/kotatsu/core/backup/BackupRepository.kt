package org.koitharu.kotatsu.core.backup

import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.parsers.util.json.JSONIterator
import org.koitharu.kotatsu.parsers.util.json.mapJSON
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
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

	suspend fun dumpBookmarks(): BackupEntry {
		val entry = BackupEntry(BackupEntry.BOOKMARKS, JSONArray())
		val all = db.bookmarksDao.findAll()
		for ((m, b) in all) {
			val json = JSONObject()
			val manga = JsonSerializer(m.manga).toJson()
			json.put("manga", manga)
			val tags = JSONArray()
			m.tags.forEach { tags.put(JsonSerializer(it).toJson()) }
			json.put("tags", tags)
			val bookmarks = JSONArray()
			b.forEach { bookmarks.put(JsonSerializer(it).toJson()) }
			json.put("bookmarks", bookmarks)
			entry.data.put(json)
		}
		return entry
	}

	fun dumpSettings(): BackupEntry {
		val entry = BackupEntry(BackupEntry.SETTINGS, JSONArray())
		val settingsDump = settings.getAllValues().toMutableMap()
		settingsDump.remove(AppSettings.KEY_APP_PASSWORD)
		settingsDump.remove(AppSettings.KEY_PROXY_PASSWORD)
		settingsDump.remove(AppSettings.KEY_PROXY_LOGIN)
		settingsDump.remove(AppSettings.KEY_INCOGNITO_MODE)
		val json = JsonSerializer(settingsDump).toJson()
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

	suspend fun restoreBookmarks(entry: BackupEntry): CompositeResult {
		val result = CompositeResult()
		for (item in entry.data.JSONIterator()) {
			val mangaJson = item.getJSONObject("manga")
			val manga = JsonDeserializer(mangaJson).toMangaEntity()
			val tags = item.getJSONArray("tags").mapJSON {
				JsonDeserializer(it).toTagEntity()
			}
			val bookmarks = item.getJSONArray("bookmarks").mapJSON {
				JsonDeserializer(it).toBookmarkEntity()
			}
			result += runCatchingCancellable {
				db.withTransaction {
					db.tagsDao.upsert(tags)
					db.mangaDao.upsert(manga, tags)
					db.bookmarksDao.upsert(bookmarks)
				}
			}
		}
		return result
	}

	fun restoreSettings(entry: BackupEntry): CompositeResult {
		val result = CompositeResult()
		for (item in entry.data.JSONIterator()) {
			result += runCatchingCancellable {
				settings.upsertAll(JsonDeserializer(item).toMap())
			}
		}
		return result
	}
}
