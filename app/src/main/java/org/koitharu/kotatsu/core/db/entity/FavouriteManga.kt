package org.koitharu.kotatsu.core.db.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class FavouriteManga(
	@Embedded val favourite: FavouriteEntity,
	@Relation(
		parentColumn = "manga_id",
		entityColumn = "manga_id"
	)
	val manga: MangaEntity,
	@Relation(
		parentColumn = "category_id",
		entityColumn = "category_id"
	)
	val categories: List<FavouriteCategoryEntity>,
	@Relation(
		parentColumn = "manga_id",
		entityColumn = "tag_id",
		associateBy = Junction(MangaTagsEntity::class)
	)
	val tags: List<TagEntity>
)