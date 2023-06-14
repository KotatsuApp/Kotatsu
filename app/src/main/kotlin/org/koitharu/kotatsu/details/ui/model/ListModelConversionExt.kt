package org.koitharu.kotatsu.details.ui.model

import org.koitharu.kotatsu.details.ui.model.ChapterListItem.Companion.FLAG_BOOKMARKED
import org.koitharu.kotatsu.details.ui.model.ChapterListItem.Companion.FLAG_CURRENT
import org.koitharu.kotatsu.details.ui.model.ChapterListItem.Companion.FLAG_DOWNLOADED
import org.koitharu.kotatsu.details.ui.model.ChapterListItem.Companion.FLAG_NEW
import org.koitharu.kotatsu.details.ui.model.ChapterListItem.Companion.FLAG_UNREAD
import org.koitharu.kotatsu.parsers.model.MangaChapter

fun MangaChapter.toListItem(
	isCurrent: Boolean,
	isUnread: Boolean,
	isNew: Boolean,
	isDownloaded: Boolean,
	isBookmarked: Boolean,
): ChapterListItem {
	var flags = 0
	if (isCurrent) flags = flags or FLAG_CURRENT
	if (isUnread) flags = flags or FLAG_UNREAD
	if (isNew) flags = flags or FLAG_NEW
	if (isBookmarked) flags = flags or FLAG_BOOKMARKED
	if (isDownloaded) flags = flags or FLAG_DOWNLOADED
	return ChapterListItem(
		chapter = this,
		flags = flags,
		uploadDateMs = uploadDate,
	)
}
