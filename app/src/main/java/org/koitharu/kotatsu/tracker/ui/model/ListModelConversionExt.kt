package org.koitharu.kotatsu.tracker.ui.model

import android.content.res.Resources
import android.text.format.DateUtils
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.TrackingLogItem
import org.koitharu.kotatsu.utils.ext.formatRelative

fun TrackingLogItem.toFeedItem(resources: Resources) = FeedItem(
	id = id,
	imageUrl = manga.coverUrl,
	title = manga.title,
	subtitle = buildString {
		append(createdAt.formatRelative(DateUtils.DAY_IN_MILLIS))
		append(" ")
		append(
			resources.getQuantityString(
				R.plurals.new_chapters,
				chapters.size,
				chapters.size
			)
		)
	},
	chapters = chapters.joinToString("\n"),
	manga = manga
)