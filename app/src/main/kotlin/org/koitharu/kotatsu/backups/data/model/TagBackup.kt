package org.koitharu.kotatsu.backups.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koitharu.kotatsu.core.db.entity.TagEntity

@Serializable
class TagBackup(
	@SerialName("id") val id: Long,
	@SerialName("title") val title: String,
	@SerialName("key") val key: String,
	@SerialName("source") val source: String,
	@SerialName("pinned") val isPinned: Boolean = false,
) {

	constructor(entity: TagEntity) : this(
		id = entity.id,
		title = entity.title,
		key = entity.key,
		source = entity.source,
		isPinned = entity.isPinned,
	)

	fun toEntity() = TagEntity(
		id = id,
		title = title,
		key = key,
		source = source,
		isPinned = isPinned,
	)
}
