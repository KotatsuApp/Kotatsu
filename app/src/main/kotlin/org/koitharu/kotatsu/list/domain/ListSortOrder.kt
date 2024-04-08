package org.koitharu.kotatsu.list.domain

import androidx.annotation.StringRes
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.parsers.util.find
import java.util.EnumSet

enum class ListSortOrder(
	@StringRes val titleResId: Int,
) {

	NEWEST(R.string.order_added),
	OLDEST(R.string.order_oldest),
	PROGRESS(R.string.progress),
	UNREAD(R.string.unread),
	ALPHABETIC(R.string.by_name),
	ALPHABETIC_REVERSE(R.string.by_name_reverse),
	RATING(R.string.by_rating),
	RELEVANCE(R.string.by_relevance),
	NEW_CHAPTERS(R.string.new_chapters),
	LAST_READ(R.string.last_read),
	LONG_AGO_READ(R.string.long_ago_read),
	UPDATED(R.string.updated),
	;

	fun isGroupingSupported() = this == LAST_READ || this == NEWEST || this == PROGRESS

	companion object {

		val HISTORY: Set<ListSortOrder> = EnumSet.of(
			LAST_READ,
			LONG_AGO_READ,
			NEWEST,
			OLDEST,
			PROGRESS,
			UNREAD,
			ALPHABETIC,
			ALPHABETIC_REVERSE,
			NEW_CHAPTERS,
			UPDATED,
		)
		val FAVORITES: Set<ListSortOrder> = EnumSet.of(
			ALPHABETIC,
			ALPHABETIC_REVERSE,
			NEWEST,
			OLDEST,
			RATING,
			NEW_CHAPTERS,
			PROGRESS,
			UNREAD,
			LAST_READ,
			LONG_AGO_READ,
			UPDATED,
		)
		val SUGGESTIONS: Set<ListSortOrder> = EnumSet.of(RELEVANCE)

		operator fun invoke(value: String, fallback: ListSortOrder) = entries.find(value) ?: fallback
	}
}
