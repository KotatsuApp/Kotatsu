package org.koitharu.kotatsu.core.db.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

class MangaWithTags(
	@Embedded val manga: MangaEntity,
	@Relation(
		parentColumn = "manga_id",
		entityColumn = "tag_id",
		associateBy = Junction(MangaTagsEntity::class)
	)
	val tags: List<TagEntity>,
) {

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as MangaWithTags

		if (manga != other.manga) return false
		if (tags != other.tags) return false

		return true
	}

	override fun hashCode(): Int {
		var result = manga.hashCode()
		result = 31 * result + tags.hashCode()
		return result
	}
}