package org.koitharu.kotatsu.tracker.ui.model

import android.content.res.Resources
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.TrackingLogItem

fun TrackingLogItem.toFeedItem(resources: Resources): FeedItem {
	val chaptersString = if (chapters.size > MAX_CHAPTERS) {
		chapters.joinToString(
			separator = "\n",
			limit = MAX_CHAPTERS - 1,
			truncated = resources.getString(
				R.string._and_x_more,
				chapters.size - MAX_CHAPTERS + 1
			)
		)
	} else {
		chapters.joinToString("\n")
	}
	return FeedItem(
		id = id,
		imageUrl = manga.coverUrl,
		title = manga.title,
		subtitle = chapters.size.toString(),
		chapters = chaptersString,
		manga = manga
	)
}

private const val MAX_CHAPTERS = 6