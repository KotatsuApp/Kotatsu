package org.koitharu.kotatsu.details.ui.model

import android.text.format.DateUtils
import org.koitharu.kotatsu.parsers.model.MangaChapter

class ChapterListItem(
	val chapter: MangaChapter,
	val flags: Int,
	private val uploadDateMs: Long,
) {

	var uploadDate: CharSequence? = null
		private set
		get() {
			if (field != null) return field
			if (uploadDateMs == 0L) return null
			field = DateUtils.getRelativeTimeSpanString(
				uploadDateMs,
				System.currentTimeMillis(),
				DateUtils.DAY_IN_MILLIS,
			)
			return field
		}

	val isCurrent: Boolean
		get() = hasFlag(FLAG_CURRENT)

	val isUnread: Boolean
		get() = hasFlag(FLAG_UNREAD)

	val isDownloaded: Boolean
		get() = hasFlag(FLAG_DOWNLOADED)

	val isBookmarked: Boolean
		get() = hasFlag(FLAG_BOOKMARKED)

	val isNew: Boolean
		get() = hasFlag(FLAG_NEW)

	fun description(): CharSequence? {
		val scanlator = chapter.scanlator?.takeUnless { it.isBlank() }
		return when {
			uploadDate != null && scanlator != null -> "$uploadDate • $scanlator"
			scanlator != null -> scanlator
			else -> uploadDate
		}
	}

	private fun hasFlag(flag: Int): Boolean {
		return (flags and flag) == flag
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as ChapterListItem

		if (chapter != other.chapter) return false
		if (flags != other.flags) return false
		return uploadDateMs == other.uploadDateMs
	}

	override fun hashCode(): Int {
		var result = chapter.hashCode()
		result = 31 * result + flags
		result = 31 * result + uploadDateMs.hashCode()
		return result
	}

	companion object {

		const val FLAG_UNREAD = 2
		const val FLAG_CURRENT = 4
		const val FLAG_NEW = 8
		const val FLAG_BOOKMARKED = 16
		const val FLAG_DOWNLOADED = 32
	}
}
