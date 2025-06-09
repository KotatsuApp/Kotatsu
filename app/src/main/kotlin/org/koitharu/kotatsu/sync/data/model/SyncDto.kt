package org.koitharu.kotatsu.sync.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SyncDto(
	@SerialName("history") val history: List<HistorySyncDto>?,
	@SerialName("categories") val categories: List<FavouriteCategorySyncDto>?,
	@SerialName("favourites") val favourites: List<FavouriteSyncDto>?,
	@SerialName("timestamp") val timestamp: Long,
)
