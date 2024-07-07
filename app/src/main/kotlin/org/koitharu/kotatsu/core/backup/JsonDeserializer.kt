package org.koitharu.kotatsu.core.backup

import org.json.JSONObject
import org.koitharu.kotatsu.bookmarks.data.BookmarkEntity
import org.koitharu.kotatsu.core.db.entity.MangaEntity
import org.koitharu.kotatsu.core.db.entity.MangaSourceEntity
import org.koitharu.kotatsu.core.db.entity.TagEntity
import org.koitharu.kotatsu.favourites.data.FavouriteCategoryEntity
import org.koitharu.kotatsu.favourites.data.FavouriteEntity
import org.koitharu.kotatsu.history.data.HistoryEntity
import org.koitharu.kotatsu.parsers.model.SortOrder
import org.koitharu.kotatsu.parsers.util.json.getBooleanOrDefault
import org.koitharu.kotatsu.parsers.util.json.getFloatOrDefault
import org.koitharu.kotatsu.parsers.util.json.getIntOrDefault
import org.koitharu.kotatsu.parsers.util.json.getLongOrDefault
import org.koitharu.kotatsu.parsers.util.json.getStringOrNull

class JsonDeserializer(private val json: JSONObject) {

	fun toFavouriteEntity() = FavouriteEntity(
		mangaId = json.getLong("manga_id"),
		categoryId = json.getLong("category_id"),
		sortKey = json.getIntOrDefault("sort_key", 0),
		createdAt = json.getLong("created_at"),
		deletedAt = 0L,
	)

	fun toMangaEntity() = MangaEntity(
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
		source = json.getString("source"),
	)

	fun toTagEntity() = TagEntity(
		id = json.getLong("id"),
		title = json.getString("title"),
		key = json.getString("key"),
		source = json.getString("source"),
	)

	fun toHistoryEntity() = HistoryEntity(
		mangaId = json.getLong("manga_id"),
		createdAt = json.getLong("created_at"),
		updatedAt = json.getLong("updated_at"),
		chapterId = json.getLong("chapter_id"),
		page = json.getInt("page"),
		scroll = json.getDouble("scroll").toFloat(),
		percent = json.getFloatOrDefault("percent", -1f),
		chaptersCount = json.getIntOrDefault("chapters", -1),
		deletedAt = 0L,
	)

	fun toFavouriteCategoryEntity() = FavouriteCategoryEntity(
		categoryId = json.getInt("category_id"),
		createdAt = json.getLong("created_at"),
		sortKey = json.getInt("sort_key"),
		title = json.getString("title"),
		order = json.getStringOrNull("order") ?: SortOrder.NEWEST.name,
		track = json.getBooleanOrDefault("track", true),
		isVisibleInLibrary = json.getBooleanOrDefault("show_in_lib", true),
		deletedAt = 0L,
	)

	fun toBookmarkEntity() = BookmarkEntity(
		mangaId = json.getLong("manga_id"),
		pageId = json.getLong("page_id"),
		chapterId = json.getLong("chapter_id"),
		page = json.getInt("page"),
		scroll = json.getInt("scroll"),
		imageUrl = json.getString("image_url"),
		createdAt = json.getLong("created_at"),
		percent = json.getDouble("percent").toFloat(),
	)

	fun toMangaSourceEntity() = MangaSourceEntity(
		source = json.getString("source"),
		isEnabled = json.getBoolean("enabled"),
		sortKey = json.getInt("sort_key"),
		addedIn = json.getIntOrDefault("added_in", 0),
		lastUsedAt = json.getLongOrDefault("used_at", 0L),
		isPinned = json.getBooleanOrDefault("pinned", false),
	)

	fun toMap(): Map<String, Any?> {
		val map = mutableMapOf<String, Any?>()
		val keys = json.keys()

		while (keys.hasNext()) {
			val key = keys.next()
			val value = json.get(key)
			map[key] = value
		}

		return map
	}
}
