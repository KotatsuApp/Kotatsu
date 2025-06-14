package org.koitharu.kotatsu.backups.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koitharu.kotatsu.core.db.entity.MangaEntity
import org.koitharu.kotatsu.core.db.entity.MangaWithTags
import org.koitharu.kotatsu.parsers.model.RATING_UNKNOWN
import org.koitharu.kotatsu.parsers.util.mapToSet

@Serializable
class MangaBackup(
	@SerialName("id") val id: Long,
	@SerialName("title") val title: String,
	@SerialName("alt_title") val altTitles: String? = null,
	@SerialName("url") val url: String,
	@SerialName("public_url") val publicUrl: String,
	@SerialName("rating") val rating: Float = RATING_UNKNOWN,
	@SerialName("nsfw") val isNsfw: Boolean = false,
	@SerialName("content_rating") val contentRating: String? = null,
	@SerialName("cover_url") val coverUrl: String,
	@SerialName("large_cover_url") val largeCoverUrl: String? = null,
	@SerialName("state") val state: String? = null,
	@SerialName("author") val authors: String? = null,
	@SerialName("source") val source: String,
	@SerialName("tags") val tags: Set<TagBackup> = emptySet(),
) {

	constructor(entity: MangaWithTags) : this(
		id = entity.manga.id,
		title = entity.manga.title,
		altTitles = entity.manga.altTitles,
		url = entity.manga.url,
		publicUrl = entity.manga.publicUrl,
		rating = entity.manga.rating,
		isNsfw = entity.manga.isNsfw,
		contentRating = entity.manga.contentRating,
		coverUrl = entity.manga.coverUrl,
		largeCoverUrl = entity.manga.largeCoverUrl,
		state = entity.manga.state,
		authors = entity.manga.authors,
		source = entity.manga.source,
		tags = entity.tags.mapToSet { TagBackup(it) },
	)

	fun toEntity() = MangaEntity(
		id = id,
		title = title,
		altTitles = altTitles,
		url = url,
		publicUrl = publicUrl,
		rating = rating,
		isNsfw = isNsfw,
		contentRating = contentRating,
		coverUrl = coverUrl,
		largeCoverUrl = largeCoverUrl,
		state = state,
		authors = authors,
		source = source,
	)
}
