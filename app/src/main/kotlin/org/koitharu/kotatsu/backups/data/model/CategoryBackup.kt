package org.koitharu.kotatsu.backups.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koitharu.kotatsu.favourites.data.FavouriteCategoryEntity
import org.koitharu.kotatsu.list.domain.ListSortOrder

@Serializable
class CategoryBackup(
	@SerialName("category_id") val categoryId: Int,
	@SerialName("created_at") val createdAt: Long,
	@SerialName("sort_key") val sortKey: Int,
	@SerialName("title") val title: String,
	@SerialName("order") val order: String = ListSortOrder.NEWEST.name,
	@SerialName("track") val track: Boolean = true,
	@SerialName("show_in_lib") val isVisibleInLibrary: Boolean = true,
) {

	constructor(entity: FavouriteCategoryEntity) : this(
		categoryId = entity.categoryId,
		createdAt = entity.createdAt,
		sortKey = entity.sortKey,
		title = entity.title,
		order = entity.order,
		track = entity.track,
		isVisibleInLibrary = entity.isVisibleInLibrary,
	)

	fun toEntity() = FavouriteCategoryEntity(
		categoryId = categoryId,
		createdAt = createdAt,
		sortKey = sortKey,
		title = title,
		order = order,
		track = track,
		isVisibleInLibrary = isVisibleInLibrary,
		deletedAt = 0L,
	)
}
