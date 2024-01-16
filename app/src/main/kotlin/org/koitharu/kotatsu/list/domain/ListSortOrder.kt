package org.koitharu.kotatsu.list.domain

import androidx.annotation.StringRes
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.parsers.util.find
import java.util.EnumSet

enum class ListSortOrder(
	@StringRes val titleResId: Int,
) {

	UPDATED(R.string.updated),
	NEWEST(R.string.order_added),
	PROGRESS(R.string.progress),
	ALPHABETIC(R.string.by_name),
	ALPHABETIC_REVERSE(R.string.by_name_reverse),
	RATING(R.string.by_rating),
	RELEVANCE(R.string.by_relevance),
	NEW_CHAPTERS(R.string.new_chapters),
	;

	fun isGroupingSupported() = this == UPDATED || this == NEWEST || this == PROGRESS

	companion object {

		val HISTORY: Set<ListSortOrder> = EnumSet.of(UPDATED, NEWEST, PROGRESS, ALPHABETIC, ALPHABETIC_REVERSE, NEW_CHAPTERS)
		val FAVORITES: Set<ListSortOrder> = EnumSet.of(ALPHABETIC, ALPHABETIC_REVERSE, NEWEST, RATING, NEW_CHAPTERS, PROGRESS)
		val SUGGESTIONS: Set<ListSortOrder> = EnumSet.of(RELEVANCE)

		operator fun invoke(value: String, fallback: ListSortOrder) = entries.find(value) ?: fallback
	}
}
