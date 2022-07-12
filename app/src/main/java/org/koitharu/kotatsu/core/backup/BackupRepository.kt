package org.koitharu.kotatsu.core.backup

import org.json.JSONArray
import org.json.JSONObject
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.db.entity.MangaEntity
import org.koitharu.kotatsu.core.db.entity.TagEntity
import org.koitharu.kotatsu.favourites.data.FavouriteCategoryEntity
import org.koitharu.kotatsu.favourites.data.FavouriteEntity
import org.koitharu.kotatsu.history.data.HistoryEntity

private const val PAGE_SIZE = 10

class BackupRepository(private val db: MangaDatabase) {

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
				val manga = item.manga.toJson()
				val tags = JSONArray()
				item.tags.forEach { tags.put(it.toJson()) }
				manga.put("tags", tags)
				val json = item.history.toJson()
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
			entry.data.put(item.toJson())
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
				val manga = item.manga.toJson()
				val tags = JSONArray()
				item.tags.forEach { tags.put(it.toJson()) }
				manga.put("tags", tags)
				val json = item.favourite.toJson()
				json.put("manga", manga)
				entry.data.put(json)
			}
		}
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

	private fun MangaEntity.toJson(): JSONObject {
		val jo = JSONObject()
		jo.put("id", id)
		jo.put("title", title)
		jo.put("alt_title", altTitle)
		jo.put("url", url)
		jo.put("public_url", publicUrl)
		jo.put("rating", rating)
		jo.put("nsfw", isNsfw)
		jo.put("cover_url", coverUrl)
		jo.put("large_cover_url", largeCoverUrl)
		jo.put("state", state)
		jo.put("author", author)
		jo.put("source", source)
		return jo
	}

	private fun TagEntity.toJson(): JSONObject {
		val jo = JSONObject()
		jo.put("id", id)
		jo.put("title", title)
		jo.put("key", key)
		jo.put("source", source)
		return jo
	}

	private fun HistoryEntity.toJson(): JSONObject {
		val jo = JSONObject()
		jo.put("manga_id", mangaId)
		jo.put("created_at", createdAt)
		jo.put("updated_at", updatedAt)
		jo.put("chapter_id", chapterId)
		jo.put("page", page)
		jo.put("scroll", scroll)
		jo.put("percent", percent)
		return jo
	}

	private fun FavouriteCategoryEntity.toJson(): JSONObject {
		val jo = JSONObject()
		jo.put("category_id", categoryId)
		jo.put("created_at", createdAt)
		jo.put("sort_key", sortKey)
		jo.put("title", title)
		jo.put("order", order)
		jo.put("track", track)
		jo.put("show_in_lib", isVisibleInLibrary)
		return jo
	}

	private fun FavouriteEntity.toJson(): JSONObject {
		val jo = JSONObject()
		jo.put("manga_id", mangaId)
		jo.put("category_id", categoryId)
		jo.put("created_at", createdAt)
		jo.put("sort_key", sortKey)
		return jo
	}
}