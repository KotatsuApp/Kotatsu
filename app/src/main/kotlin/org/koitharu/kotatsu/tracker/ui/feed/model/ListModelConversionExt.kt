package org.koitharu.kotatsu.tracker.ui.feed.model

import org.koitharu.kotatsu.tracker.domain.model.TrackingLogItem

fun TrackingLogItem.toFeedItem() = FeedItem(
	id = id,
	imageUrl = manga.coverUrl,
	title = manga.title,
	count = chapters.size,
	manga = manga,
	isNew = isNew,
)
