package org.koitharu.kotatsu.sync.data.model

import android.database.Cursor
import androidx.core.database.getStringOrNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koitharu.kotatsu.core.util.ext.buildContentValues

@Serializable
data class MangaSyncDto(
	@SerialName("manga_id") val id: Long,
	@SerialName("title") val title: String,
	@SerialName("alt_title") val altTitle: String?,
	@SerialName("url") val url: String,
	@SerialName("public_url") val publicUrl: String,
	@SerialName("rating") val rating: Float,
	@SerialName("content_rating") val contentRating: String?,
	@SerialName("cover_url") val coverUrl: String,
	@SerialName("large_cover_url") val largeCoverUrl: String?,
	@SerialName("tags") val tags: Set<MangaTagSyncDto>,
	@SerialName("state") val state: String?,
	@SerialName("author") val author: String?,
	@SerialName("source") val source: String,
) {

	constructor(cursor: Cursor, tags: Set<MangaTagSyncDto>) : this(
		id = cursor.getLong(cursor.getColumnIndexOrThrow("manga_id")),
		title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
		altTitle = cursor.getStringOrNull(cursor.getColumnIndexOrThrow("alt_title")),
		url = cursor.getString(cursor.getColumnIndexOrThrow("url")),
		publicUrl = cursor.getString(cursor.getColumnIndexOrThrow("public_url")),
		rating = cursor.getFloat(cursor.getColumnIndexOrThrow("rating")),
		contentRating = cursor.getStringOrNull(cursor.getColumnIndexOrThrow("content_rating")),
		coverUrl = cursor.getString(cursor.getColumnIndexOrThrow("cover_url")),
		largeCoverUrl = cursor.getStringOrNull(cursor.getColumnIndexOrThrow("large_cover_url")),
		tags = tags,
		state = cursor.getStringOrNull(cursor.getColumnIndexOrThrow("state")),
		author = cursor.getStringOrNull(cursor.getColumnIndexOrThrow("author")),
		source = cursor.getString(cursor.getColumnIndexOrThrow("source")),
	)

	fun toContentValues() = buildContentValues(12) {
		put("manga_id", id)
		put("title", title)
		put("alt_title", altTitle)
		put("url", url)
		put("public_url", publicUrl)
		put("rating", rating)
		put("content_rating", contentRating)
		put("cover_url", coverUrl)
		put("large_cover_url", largeCoverUrl)
		put("state", state)
		put("author", author)
		put("source", source)
	}
}
