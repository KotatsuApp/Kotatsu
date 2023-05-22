package org.koitharu.kotatsu.tracker.data

import java.util.*
import org.koitharu.kotatsu.core.db.entity.toManga
import org.koitharu.kotatsu.core.db.entity.toMangaTags
import org.koitharu.kotatsu.tracker.domain.model.TrackingLogItem

fun TrackLogWithManga.toTrackingLogItem(counters: MutableMap<Long, Int>): TrackingLogItem {
	val chaptersList = trackLog.chapters.split('\n').filterNot { x -> x.isEmpty() }
	return TrackingLogItem(
		id = trackLog.id,
		chapters = chaptersList,
		manga = manga.toManga(tags.toMangaTags()),
		createdAt = Date(trackLog.createdAt),
		isNew = counters.decrement(trackLog.mangaId, chaptersList.size),
	)
}

private fun MutableMap<Long, Int>.decrement(key: Long, count: Int): Boolean {
	val counter = get(key)
	if (counter == null || counter <= 0) {
		return false
	}
	if (counter < count) {
		remove(key)
	} else {
		put(key, counter - count)
	}
	return true
}
