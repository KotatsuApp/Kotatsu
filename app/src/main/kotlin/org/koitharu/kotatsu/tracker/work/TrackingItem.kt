package org.koitharu.kotatsu.tracker.work

import org.koitharu.kotatsu.tracker.domain.model.MangaTracking

data class TrackingItem(
	val tracking: MangaTracking,
	val channelId: String?,
)
