package org.koitharu.kotatsu.details.ui

import org.koitharu.kotatsu.bookmarks.domain.Bookmark
import org.koitharu.kotatsu.core.model.MangaHistory
import org.koitharu.kotatsu.details.ui.model.ChapterListItem
import org.koitharu.kotatsu.details.ui.model.toListItem
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.util.mapToSet

fun mapChapters(
	remoteManga: Manga?,
	localManga: Manga?,
	history: MangaHistory?,
	newCount: Int,
	branch: String?,
	bookmarks: List<Bookmark>,
): List<ChapterListItem> {
	val remoteChapters = remoteManga?.getChapters(branch).orEmpty()
	val localChapters = localManga?.getChapters(branch).orEmpty()
	if (remoteChapters.isEmpty() && localChapters.isEmpty()) {
		return emptyList()
	}
	val bookmarked = bookmarks.mapToSet { it.chapterId }
	val currentId = history?.chapterId ?: 0L
	val newFrom = if (newCount == 0 || remoteChapters.isEmpty()) Int.MAX_VALUE else remoteChapters.size - newCount
	val chaptersSize = maxOf(remoteChapters.size, localChapters.size)
	val ids = buildSet(chaptersSize) {
		remoteChapters.mapTo(this) { it.id }
		localChapters.mapTo(this) { it.id }
	}
	val result = ArrayList<ChapterListItem>(chaptersSize)
	val localMap = if (localChapters.isNotEmpty()) {
		localChapters.associateByTo(LinkedHashMap(localChapters.size)) { it.id }
	} else {
		null
	}
	var isUnread = currentId !in ids
	for (chapter in remoteChapters) {
		val local = localMap?.remove(chapter.id)
		if (chapter.id == currentId) {
			isUnread = true
		}
		result += chapter.toListItem(
			isCurrent = chapter.id == currentId,
			isUnread = isUnread,
			isNew = isUnread && result.size >= newFrom,
			isDownloaded = local != null,
			isBookmarked = chapter.id in bookmarked,
		)
	}
	if (!localMap.isNullOrEmpty()) {
		for (chapter in localMap.values) {
			if (chapter.id == currentId) {
				isUnread = true
			}
			result += chapter.toListItem(
				isCurrent = chapter.id == currentId,
				isUnread = isUnread,
				isNew = false,
				isDownloaded = remoteManga != null,
				isBookmarked = chapter.id in bookmarked,
			)
		}
	}
	return result
}
