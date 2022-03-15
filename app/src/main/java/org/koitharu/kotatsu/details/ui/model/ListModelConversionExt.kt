package org.koitharu.kotatsu.details.ui.model

import org.koitharu.kotatsu.details.ui.model.ChapterListItem.Companion.FLAG_CURRENT
import org.koitharu.kotatsu.details.ui.model.ChapterListItem.Companion.FLAG_DOWNLOADED
import org.koitharu.kotatsu.details.ui.model.ChapterListItem.Companion.FLAG_MISSING
import org.koitharu.kotatsu.details.ui.model.ChapterListItem.Companion.FLAG_NEW
import org.koitharu.kotatsu.details.ui.model.ChapterListItem.Companion.FLAG_UNREAD
import org.koitharu.kotatsu.parsers.model.MangaChapter
import java.text.DateFormat

fun MangaChapter.toListItem(
	isCurrent: Boolean,
	isUnread: Boolean,
	isNew: Boolean,
	isMissing: Boolean,
	isDownloaded: Boolean,
	dateFormat: DateFormat,
): ChapterListItem {
	var flags = 0
	if (isCurrent) flags = flags or FLAG_CURRENT
	if (isUnread) flags = flags or FLAG_UNREAD
	if (isNew) flags = flags or FLAG_NEW
	if (isMissing) flags = flags or FLAG_MISSING
	if (isDownloaded) flags = flags or FLAG_DOWNLOADED
	return ChapterListItem(
		chapter = this,
		flags = flags,
		uploadDate = if (uploadDate != 0L) dateFormat.format(uploadDate) else null
	)
}