package org.koitharu.kotatsu.details.ui.model

import java.text.DateFormat
import org.koitharu.kotatsu.parsers.model.MangaChapter

class ChapterListItem(
	val chapter: MangaChapter,
	val flags: Int,
	private val uploadDateMs: Long,
	private val dateFormat: DateFormat,
) {

	var uploadDate: String? = null
		private set
		get() {
			if (field != null) return field
			if (uploadDateMs == 0L) return null
			field = dateFormat.format(uploadDateMs)
			return field
		}

	val status: Int
		get() = flags and MASK_STATUS

	fun hasFlag(flag: Int): Boolean {
		return (flags and flag) == flag
	}

	fun description(): CharSequence? {
		val scanlator = chapter.scanlator?.takeUnless { it.isBlank() }
		return when {
			uploadDate != null && scanlator != null -> "$uploadDate • $scanlator"
			scanlator != null -> scanlator
			else -> uploadDate
		}
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as ChapterListItem

		if (chapter != other.chapter) return false
		if (flags != other.flags) return false
		if (uploadDateMs != other.uploadDateMs) return false
		if (dateFormat != other.dateFormat) return false

		return true
	}

	override fun hashCode(): Int {
		var result = chapter.hashCode()
		result = 31 * result + flags
		result = 31 * result + uploadDateMs.hashCode()
		result = 31 * result + dateFormat.hashCode()
		return result
	}

	companion object {

		const val FLAG_UNREAD = 2
		const val FLAG_CURRENT = 4
		const val FLAG_NEW = 8
		const val FLAG_MISSING = 16
		const val FLAG_DOWNLOADED = 32
		const val MASK_STATUS = FLAG_UNREAD or FLAG_CURRENT
	}
}