package org.koitharu.kotatsu.tracker.ui.model

import org.koitharu.kotatsu.core.model.TrackingLogItem

fun TrackingLogItem.toFeedItem() = FeedItem(
	id = id,
	imageUrl = manga.coverUrl,
	title = manga.title,
	count = chapters.size,
	manga = manga,
)