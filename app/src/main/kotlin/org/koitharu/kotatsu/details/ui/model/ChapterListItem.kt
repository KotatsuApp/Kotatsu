package org.koitharu.kotatsu.details.ui.model

import android.text.format.DateUtils
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.parsers.model.MangaChapter

data class ChapterListItem(
	val chapter: MangaChapter,
	val flags: Int,
	private val uploadDateMs: Long,
) : ListModel {

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

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is ChapterListItem && chapter.id == other.chapter.id
	}

	override fun getChangePayload(previousState: ListModel): Any? {
		if (previousState !is ChapterListItem) {
			return super.getChangePayload(previousState)
		}
		return if (chapter == previousState.chapter && flags != previousState.flags) {
			flags
		} else {
			super.getChangePayload(previousState)
		}
	}

	companion object {

		const val FLAG_UNREAD = 2
		const val FLAG_CURRENT = 4
		const val FLAG_NEW = 8
		const val FLAG_BOOKMARKED = 16
		const val FLAG_DOWNLOADED = 32
	}
}
