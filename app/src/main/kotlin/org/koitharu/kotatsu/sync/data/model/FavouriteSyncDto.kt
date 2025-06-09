package org.koitharu.kotatsu.sync.data.model

import android.database.Cursor
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koitharu.kotatsu.core.util.ext.buildContentValues
import org.koitharu.kotatsu.core.util.ext.getBoolean

@Serializable
data class FavouriteSyncDto(
	@SerialName("manga_id") val mangaId: Long,
	@SerialName("manga") val manga: MangaSyncDto,
	@SerialName("category_id") val categoryId: Int,
	@SerialName("sort_key") val sortKey: Int,
	@SerialName("pinned") val pinned: Boolean,
	@SerialName("created_at") val createdAt: Long,
	@SerialName("deleted_at") var deletedAt: Long,
) {

	constructor(cursor: Cursor, manga: MangaSyncDto) : this(
		mangaId = cursor.getLong(cursor.getColumnIndexOrThrow("manga_id")),
		manga = manga,
		categoryId = cursor.getInt(cursor.getColumnIndexOrThrow("category_id")),
		sortKey = cursor.getInt(cursor.getColumnIndexOrThrow("sort_key")),
		pinned = cursor.getBoolean(cursor.getColumnIndexOrThrow("pinned")),
		createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
		deletedAt = cursor.getLong(cursor.getColumnIndexOrThrow("deleted_at")),
	)

	fun toContentValues() = buildContentValues(6) {
		put("manga_id", mangaId)
		put("category_id", categoryId)
		put("sort_key", sortKey)
		put("pinned", pinned)
		put("created_at", createdAt)
		put("deleted_at", deletedAt)
	}
}
