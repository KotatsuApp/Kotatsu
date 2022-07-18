package org.koitharu.kotatsu.tracker.data

import java.util.*
import org.koitharu.kotatsu.core.db.entity.toManga
import org.koitharu.kotatsu.core.db.entity.toMangaTags
import org.koitharu.kotatsu.tracker.domain.model.TrackingLogItem

fun TrackLogWithManga.toTrackingLogItem() = TrackingLogItem(
	id = trackLog.id,
	chapters = trackLog.chapters.split('\n').filterNot { x -> x.isEmpty() },
	manga = manga.toManga(tags.toMangaTags()),
	createdAt = Date(trackLog.createdAt)
)