package org.koitharu.kotatsu.backups.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koitharu.kotatsu.core.db.entity.MangaSourceEntity

@Serializable
class SourceBackup(
	@SerialName("source") val source: String,
	@SerialName("sort_key") val sortKey: Int,
	@SerialName("used_at") val lastUsedAt: Long,
	@SerialName("added_in") val addedIn: Int,
	@SerialName("pinned") val isPinned: Boolean = false,
	@SerialName("enabled") val isEnabled: Boolean = true, // for compatibility purposes, should be only true
) {

	constructor(entity: MangaSourceEntity) : this(
		source = entity.source,
		sortKey = entity.sortKey,
		lastUsedAt = entity.lastUsedAt,
		addedIn = entity.addedIn,
		isPinned = entity.isPinned,
		isEnabled = entity.isEnabled,
	)

	fun toEntity() = MangaSourceEntity(
		source = source,
		isEnabled = isEnabled,
		sortKey = sortKey,
		addedIn = addedIn,
		lastUsedAt = lastUsedAt,
		isPinned = isPinned,
		cfState = 0,
	)
}
