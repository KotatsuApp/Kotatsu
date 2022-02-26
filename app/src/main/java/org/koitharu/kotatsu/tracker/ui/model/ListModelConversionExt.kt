package org.koitharu.kotatsu.tracker.ui.model

import org.koitharu.kotatsu.core.model.TrackingLogItem

fun TrackingLogItem.toFeedItem(): FeedItem {
	val truncate = chapters.size > MAX_CHAPTERS
	val chaptersString = if (truncate) {
		chapters.joinToString(
			separator = "\n",
			limit = MAX_CHAPTERS - 1,
			truncated = "",
		).trimEnd()
	} else {
		chapters.joinToString("\n")
	}
	return FeedItem(
		id = id,
		imageUrl = manga.coverUrl,
		title = manga.title,
		subtitle = chapters.size.toString(),
		chapters = chaptersString,
		manga = manga,
		truncated = chapters.size - MAX_CHAPTERS + 1,
	)
}

private const val MAX_CHAPTERS = 6