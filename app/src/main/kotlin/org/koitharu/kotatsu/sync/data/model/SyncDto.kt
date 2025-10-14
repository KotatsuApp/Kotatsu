package org.koitharu.kotatsu.sync.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SyncDto(
	@SerialName("history") val history: List<HistorySyncDto>? = null,
	@SerialName("categories") val categories: List<FavouriteCategorySyncDto>? = null,
	@SerialName("favourites") val favourites: List<FavouriteSyncDto>? = null,
	@SerialName("timestamp") val timestamp: Long,
)
