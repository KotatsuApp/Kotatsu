package org.koitharu.kotatsu.core.backup

import androidx.room.withTransaction
import org.json.JSONObject
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.db.entity.MangaEntity
import org.koitharu.kotatsu.core.db.entity.TagEntity
import org.koitharu.kotatsu.favourites.data.FavouriteCategoryEntity
import org.koitharu.kotatsu.favourites.data.FavouriteEntity
import org.koitharu.kotatsu.history.data.HistoryEntity
import org.koitharu.kotatsu.utils.ext.getBooleanOrDefault
import org.koitharu.kotatsu.utils.ext.getStringOrNull
import org.koitharu.kotatsu.utils.ext.iterator
import org.koitharu.kotatsu.utils.ext.map

class RestoreRepository(private val db: MangaDatabase) {

	suspend fun upsertHistory(entry: BackupEntry): CompositeResult {
		val result = CompositeResult()
		for (item in entry.data) {
			val mangaJson = item.getJSONObject("manga")
			val manga = parseManga(mangaJson)
			val tags = mangaJson.getJSONArray("tags").map {
				parseTag(it)
			}
			val history = parseHistory(item)
			result += runCatching {
				db.withTransaction {
					db.tagsDao.upsert(tags)
					db.mangaDao.upsert(manga, tags)
					db.historyDao.upsert(history)
				}
			}
		}
		return result
	}

	suspend fun upsertCategories(entry: BackupEntry): CompositeResult {
		val result = CompositeResult()
		for (item in entry.data) {
			val category = parseCategory(item)
			result += runCatching {
				db.favouriteCategoriesDao.upsert(category)
			}
		}
		return result
	}

	suspend fun upsertFavourites(entry: BackupEntry): CompositeResult {
		val result = CompositeResult()
		for (item in entry.data) {
			val mangaJson = item.getJSONObject("manga")
			val manga = parseManga(mangaJson)
			val tags = mangaJson.getJSONArray("tags").map {
				parseTag(it)
			}
			val favourite = parseFavourite(item)
			result += runCatching {
				db.withTransaction {
					db.tagsDao.upsert(tags)
					db.mangaDao.upsert(manga, tags)
					db.favouritesDao.upsert(favourite)
				}
			}
		}
		return result
	}

	private fun parseManga(json: JSONObject) = MangaEntity(
		id = json.getLong("id"),
		title = json.getString("title"),
		altTitle = json.getStringOrNull("alt_title"),
		url = json.getString("url"),
		publicUrl = json.getStringOrNull("public_url").orEmpty(),
		rating = json.getDouble("rating").toFloat(),
		isNsfw = json.getBooleanOrDefault("nsfw", false),
		coverUrl = json.getString("cover_url"),
		largeCoverUrl = json.getStringOrNull("large_cover_url"),
		state = json.getStringOrNull("state"),
		author = json.getStringOrNull("author"),
		source = json.getString("source")
	)

	private fun parseTag(json: JSONObject) = TagEntity(
		id = json.getLong("id"),
		title = json.getString("title"),
		key = json.getString("key"),
		source = json.getString("source")
	)

	private fun parseHistory(json: JSONObject) = HistoryEntity(
		mangaId = json.getLong("manga_id"),
		createdAt = json.getLong("created_at"),
		updatedAt = json.getLong("updated_at"),
		chapterId = json.getLong("chapter_id"),
		page = json.getInt("page"),
		scroll = json.getDouble("scroll").toFloat()
	)

	private fun parseCategory(json: JSONObject) = FavouriteCategoryEntity(
		categoryId = json.getInt("category_id"),
		createdAt = json.getLong("created_at"),
		sortKey = json.getInt("sort_key"),
		title = json.getString("title")
	)

	private fun parseFavourite(json: JSONObject) = FavouriteEntity(
		mangaId = json.getLong("manga_id"),
		categoryId = json.getLong("category_id"),
		createdAt = json.getLong("created_at")
	)
}